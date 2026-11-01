package ess.ll.client.features.combat;

public final class GrimAuraRotation implements AuraRotationStrategy {
    @Override
    public String name() {
        return "Grim";
    }

    @Override
    public void calculateRotation(AuraRotationController.RotationContext var1, AuraRotationController.RotationStep var2) {
        var2.useUnlimitedSpeeds();
    }
}
