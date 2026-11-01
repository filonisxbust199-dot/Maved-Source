#requires -version 5
# Media bridge for the Dynamic Island: prints the current Windows media session (SMTC) as one JSON line
# per tick and runs commands (play, pause, toggle, next, previous, seek <ms>) written to CommandPath.
param(
    [int]    $IntervalMs = 400,
    [string] $ArtPath = "$env:TEMP\ess_media_art.png",
    [string] $CommandPath = "$env:TEMP\ess_media_cmd.txt"
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime

$asTaskMethod = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq 'AsTask' -and
        $_.GetParameters().Count -eq 1 -and
        $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
    })[0]

function Wait-Async($operation, $resultType) {
    $task = $asTaskMethod.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (-not $task.Wait(4000)) { throw 'WinRT operation timed out' }
    return $task.Result
}

$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]

$asStreamMethod = [System.IO.WindowsRuntimeStreamExtensions].GetMethod(
    'AsStreamForRead', [type[]] @([Windows.Storage.Streams.IInputStream]))

$managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$manager = Wait-Async ($managerType::RequestAsync()) ($managerType)

function Save-Thumbnail($properties, $path) {
    if ($null -eq $properties.Thumbnail) { return $false }
    $winrtStream = Wait-Async ($properties.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
    $netStream = $asStreamMethod.Invoke($null, @($winrtStream))
    try {
        $buffer = New-Object System.IO.MemoryStream
        try {
            $netStream.CopyTo($buffer)
            $bytes = $buffer.ToArray()
        } finally {
            $buffer.Dispose()
        }
        if ($bytes.Length -le 0) { return $false }
        $temporary = "$path.tmp"
        [System.IO.File]::WriteAllBytes($temporary, $bytes)
        Move-Item -LiteralPath $temporary -Destination $path -Force
        return $true
    } finally {
        $netStream.Dispose()
    }
}

function Invoke-MediaAction([string] $name, [long] $value) {
    $session = $manager.GetCurrentSession()
    if ($null -eq $session) { return }
    switch ($name) {
        'play' { $null = Wait-Async ($session.TryPlayAsync()) ([bool]) }
        'pause' { $null = Wait-Async ($session.TryPauseAsync()) ([bool]) }
        'toggle' { $null = Wait-Async ($session.TryTogglePlayPauseAsync()) ([bool]) }
        'next' { $null = Wait-Async ($session.TrySkipNextAsync()) ([bool]) }
        'previous' { $null = Wait-Async ($session.TrySkipPreviousAsync()) ([bool]) }
        'seek' { $null = Wait-Async ($session.TryChangePlaybackPositionAsync([long] ($value * 10000))) ([bool]) }
    }
}

$stdout = New-Object System.IO.StreamWriter([Console]::OpenStandardOutput(), (New-Object System.Text.UTF8Encoding($false)))
$stdout.AutoFlush = $true

function Write-State($state) {
    $stdout.WriteLine(($state | ConvertTo-Json -Compress))
}

function Read-Commands {
    if (-not (Test-Path -LiteralPath $CommandPath)) { return }
    try {
        $lines = @(Get-Content -LiteralPath $CommandPath -Encoding UTF8)
        Remove-Item -LiteralPath $CommandPath -Force
    } catch {
        return
    }
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $parts = $line.Trim().Split(' ')
        $value = 0
        if ($parts.Count -gt 1) { [void][long]::TryParse($parts[1], [ref] $value) }
        try { Invoke-MediaAction $parts[0] $value } catch { }
    }
}

function Publish-State {
    $session = $manager.GetCurrentSession()
    if ($null -eq $session) {
        $script:lastThumbId = ''
        Write-State ([pscustomobject] @{ ok = $false; reason = 'no-session' })
        return
    }
    $properties = Wait-Async ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
    $timeline = $session.GetTimelineProperties()
    $playback = $session.GetPlaybackInfo()

    $title = [string] $properties.Title
    $artist = [string] $properties.Artist
    if ([string]::IsNullOrWhiteSpace($artist)) { $artist = [string] $properties.AlbumArtist }

    $thumbId = "$title|$artist".GetHashCode().ToString()
    if ($thumbId -ne $script:lastThumbId) {
        $saved = $false
        try { $saved = Save-Thumbnail $properties $ArtPath } catch { $saved = $false }
        $script:lastThumbId = if ($saved) { $thumbId } else { 'none' }
    }

    $age = 0
    if ($timeline.LastUpdatedTime.Year -gt 1601) {
        $age = [long] ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalMilliseconds
        if ($age -lt 0) { $age = 0 }
    }

    Write-State ([pscustomobject] @{
            ok       = $true
            title    = $title
            artist   = $artist
            album    = [string] $properties.AlbumTitle
            app      = [string] $session.SourceAppUserModelId
            status   = [string] $playback.PlaybackStatus
            position = [long] $timeline.Position.TotalMilliseconds
            duration = [long] ($timeline.EndTime.TotalMilliseconds - $timeline.StartTime.TotalMilliseconds)
            age      = $age
            canSeek  = [bool] $playback.Controls.IsPlaybackPositionEnabled
            thumb    = if ($script:lastThumbId -eq 'none') { '' } else { $ArtPath }
            thumbId  = $script:lastThumbId
        })
}

$lastThumbId = ''
$commandChecks = 8
$sleepMs = [Math]::Max(50, [int] ($IntervalMs / $commandChecks))

while ($true) {
    try {
        Publish-State
    } catch {
        Write-State ([pscustomobject] @{ ok = $false; reason = [string] $_.Exception.Message })
    }
    for ($check = 0; $check -lt $commandChecks; $check++) {
        Start-Sleep -Milliseconds $sleepMs
        Read-Commands
    }
}
