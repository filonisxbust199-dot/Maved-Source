package ess.ll.client.core;

import ess.ll.client.util.ElapsedTimer;
import ess.ll.client.util.SortableArrayList;
import java.util.function.BooleanSupplier;

public class TickTaskQueue {
   private final ElapsedTimer timer = new ElapsedTimer();
   private final SortableArrayList<TickTaskQueue.TimedTask> timedTasks = new SortableArrayList<>();
   private final SortableArrayList<TickTaskQueue.TickTask> tickTasks = new SortableArrayList<>();
   private int timedIndex;
   private int tickIndex;
   private boolean paused;
   private TickTaskQueue.RepeatPolicy repeatPolicy = new TickTaskQueue.RepeatTimes(1);

   public int getTimedIndex() {
      return this.timedIndex;
   }

   public TickTaskQueue addTickTask(int ticks, TickTaskQueue.Action action) {
      return this.addTickTask(ticks, action, () -> true, 0);
   }

   public SortableArrayList<TickTaskQueue.TickTask> getTickTasks() {
      return this.tickTasks;
   }

   public void tick() {
      if ((!this.timedTasks.isEmpty() || !this.tickTasks.isEmpty()) && !this.paused) {
         this.runTimedTasks();
         this.runTickTasks();
         this.timedIndex = Math.min(this.timedIndex, this.timedTasks.size());
         this.tickIndex = Math.min(this.tickIndex, this.tickTasks.size());
      }
   }

   public void setTimedIndex(int timedIndex) {
      this.timedIndex = timedIndex;
   }

   public ElapsedTimer getTimer() {
      return this.timer;
   }

   public boolean isFinished() {
      return this.timedTasks.isEmpty() && this.tickTasks.isEmpty()
         ? !this.paused
         : this.timedIndex >= this.timedTasks.size() && this.tickIndex >= this.tickTasks.size() && !this.paused && this.repeatPolicy.isDone();
   }

   public TickTaskQueue addTickTask(int ticks, TickTaskQueue.Action action, BooleanSupplier booleanSupplier) {
      return this.addTickTask(ticks, action, booleanSupplier, 0);
   }

   public TickTaskQueue addTickTask(int ticks, TickTaskQueue.Action action, BooleanSupplier booleanSupplier, int priority) {
      this.tickTasks.add(new TickTaskQueue.TickTask(ticks, action, booleanSupplier, priority));
      this.tickTasks.sortByInt(task -> -task.getPriority());
      return this;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public TickTaskQueue withRepeatPolicy(TickTaskQueue.RepeatPolicy policy) {
      this.repeatPolicy = policy;
      return this;
   }

   private void runTickTasks() {
      if (this.tickIndex < this.tickTasks.size()) {
         TickTaskQueue.TickTask task = this.tickTasks.get(this.tickIndex);
         if (task.getCondition().getAsBoolean()) {
            if (task.getRemaining() > 0) {
               task.countDown();
            }

            if (task.getRemaining() <= 0) {
               task.getAction().run();
               this.tickIndex++;
               if (this.repeatPolicy.shouldRestart(this.tickIndex, this.tickTasks.size())) {
                  this.restart();
                  this.repeatPolicy.onRestart();
               }
            }
         }
      }
   }

   public TickTaskQueue.RepeatPolicy getRepeatPolicy() {
      return this.repeatPolicy;
   }

   public void resetTimer() {
      this.timer.reset();
   }

   public TickTaskQueue addTimedTask(int delay, TickTaskQueue.Action action, int priority) {
      return this.addTimedTask(delay, action, () -> true, priority);
   }

   public TickTaskQueue clear() {
      this.timedTasks.clear();
      this.tickTasks.clear();
      this.paused = false;
      this.resetTimer();
      this.restart();
      return this;
   }

   public TickTaskQueue addTimedTask(int delay, TickTaskQueue.Action action) {
      return this.addTimedTask(delay, action, () -> true, 0);
   }

   private void runTimedTasks() {
      if (this.timedIndex < this.timedTasks.size()) {
         TickTaskQueue.TimedTask task = this.timedTasks.get(this.timedIndex);
         if (task.getCondition().getAsBoolean()) {
            if (this.timer.hasElapsed(task.getDelay())) {
               task.getAction().run();
               this.timedIndex++;
               this.resetTimer();
               if (this.repeatPolicy.shouldRestart(this.timedIndex, this.timedTasks.size())) {
                  this.restart();
                  this.repeatPolicy.onRestart();
               }
            }
         }
      }
   }

   public SortableArrayList<TickTaskQueue.TimedTask> getTimedTasks() {
      return this.timedTasks;
   }

   public void setTickIndex(int tickIndex) {
      this.tickIndex = tickIndex;
   }

   public TickTaskQueue addTimedTask(int delay, TickTaskQueue.Action action, BooleanSupplier booleanSupplier, int priority) {
      this.timedTasks.add(new TickTaskQueue.TimedTask(delay, action, booleanSupplier, priority));
      this.timedTasks.sortByInt(task -> -task.getPriority());
      return this;
   }

   public TickTaskQueue addTickTask(int ticks, TickTaskQueue.Action action, int priority) {
      return this.addTickTask(ticks, action, () -> true, priority);
   }

   public void restart() {
      this.timedIndex = 0;
      this.tickIndex = 0;
      this.tickTasks.forEach(TickTaskQueue.TickTask::reset);
   }

   public TickTaskQueue() {
      this.clear();
   }

   public int getTickIndex() {
      return this.tickIndex;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public TickTaskQueue addTimedTask(int delay, TickTaskQueue.Action action, BooleanSupplier booleanSupplier) {
      return this.addTimedTask(delay, action, booleanSupplier, 0);
   }

   public static class RepeatTimes implements TickTaskQueue.RepeatPolicy {
      private final int repeats;
      private int count;

      @Override
      public void onRestart() {
         this.count++;
      }

      @Override
      public boolean isDone() {
         return this.count >= this.repeats;
      }

      @Override
      public boolean shouldRestart(int index, int size) {
         return index >= size && this.count < this.repeats;
      }

      public RepeatTimes(int times) {
         this.repeats = times - 1;
      }
   }

   public static final class TickTask implements Comparable<TickTaskQueue.TickTask> {
      private final int ticks;
      private int remaining;
      private TickTaskQueue.Action action;
      private BooleanSupplier condition;
      private int priority;

      public BooleanSupplier getCondition() {
         return this.condition;
      }

      public int getRemaining() {
         return this.remaining;
      }

      @Override
      public int compareTo(TickTaskQueue.TickTask other) {
         return this.compare(other);
      }

      public int compare(TickTaskQueue.TickTask other) {
         return Integer.compare(other.getPriority(), this.getPriority());
      }

      public TickTaskQueue.Action getAction() {
         return this.action;
      }

      public TickTaskQueue.TickTask withCondition(BooleanSupplier booleanSupplier) {
         this.condition = booleanSupplier;
         return this;
      }

      public void countDown() {
         this.remaining--;
      }

      public TickTask(int ticks, TickTaskQueue.Action action, BooleanSupplier booleanSupplier, int priority) {
         this.ticks = Math.max(0, ticks);
         this.remaining = this.ticks;
         this.action = action;
         this.condition = booleanSupplier;
         this.priority = priority;
      }

      public TickTaskQueue.TickTask withAction(TickTaskQueue.Action action) {
         this.action = action;
         return this;
      }

      public TickTaskQueue.TickTask withPriority(int priority) {
         this.priority = priority;
         return this;
      }

      public void reset() {
         this.remaining = this.ticks;
      }

      public int getTicks() {
         return this.ticks;
      }

      public TickTaskQueue.TickTask withRemaining(int remaining) {
         this.remaining = remaining;
         return this;
      }

      public int getPriority() {
         return this.priority;
      }
   }

   public interface RepeatPolicy {
      boolean shouldRestart(int index, int size);

      void onRestart();

      boolean isDone();
   }

   public static class RepeatForever implements TickTaskQueue.RepeatPolicy {
      @Override
      public void onRestart() {
      }

      @Override
      public boolean isDone() {
         return false;
      }

      @Override
      public boolean shouldRestart(int index, int size) {
         return index >= size;
      }
   }

   @FunctionalInterface
   public interface Action {
      void run();
   }

   public static final class TimedTask implements Comparable<TickTaskQueue.TimedTask> {
      private int delay;
      private TickTaskQueue.Action action;
      private BooleanSupplier condition;
      private int priority;

      public TickTaskQueue.Action getAction() {
         return this.action;
      }

      public BooleanSupplier getCondition() {
         return this.condition;
      }

      public TickTaskQueue.TimedTask withAction(TickTaskQueue.Action action) {
         this.action = action;
         return this;
      }

      public int getDelay() {
         return this.delay;
      }

      @Override
      public int compareTo(TickTaskQueue.TimedTask other) {
         return this.compare(other);
      }

      public int compare(TickTaskQueue.TimedTask other) {
         return Integer.compare(other.getPriority(), this.getPriority());
      }

      public int getPriority() {
         return this.priority;
      }

      public TickTaskQueue.TimedTask withDelay(int delay) {
         this.delay = delay;
         return this;
      }

      public TickTaskQueue.TimedTask withPriority(int priority) {
         this.priority = priority;
         return this;
      }

      public TickTaskQueue.TimedTask withCondition(BooleanSupplier booleanSupplier) {
         this.condition = booleanSupplier;
         return this;
      }

      public TimedTask(int delay, TickTaskQueue.Action action, BooleanSupplier booleanSupplier, int priority) {
         this.delay = delay;
         this.action = action;
         this.condition = booleanSupplier;
         this.priority = priority;
      }
   }
}
