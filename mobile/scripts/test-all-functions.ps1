# Clash Verge Rev Android - Full Feature Test
# Test all core functions and generate report

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Clash Verge Rev - Full Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test results
$results = @{}

# Function: Execute test and record result
function Test-Feature {
    param(
        [string]$Name,
        [scriptblock]$TestBlock
    )
    
    Write-Host "Testing: $Name" -ForegroundColor Yellow
    try {
        $result = & $TestBlock
        if ($result) {
            Write-Host "   PASS" -ForegroundColor Green
            $results[$Name] = "PASS"
        } else {
            Write-Host "   FAIL" -ForegroundColor Red
            $results[$Name] = "FAIL"
        }
    } catch {
        Write-Host "   ERROR: $($_.Exception.Message)" -ForegroundColor Red
        $results[$Name] = "ERROR"
    }
    Write-Host ""
}

# 1. Check if app is running
Test-Feature "App Running" {
    $running = adb shell "ps | grep clash_verge_rev.debug" 2>$null
    return $running -ne $null -and $running.Length -gt 0
}

# 2. Check config files
Test-Feature "Config Files Exist" {
    $files = adb shell "run-as io.github.clash_verge_rev.clash_verge_rev.debug ls /data/user/0/io.github.clash_verge_rev.clash_verge_rev.debug/files/" 2>$null
    return ($files -match "merge.yaml") -and ($files -match "script.js")
}

# 3. Check VPN service
Test-Feature "VPN Service Started" {
    $vpnLog = adb logcat -d | Select-String "ClashVpnService.*Starting VPN" | Select-Object -Last 1
    return $vpnLog -ne $null
}

# 4. Check HTTP API server
Test-Feature "HTTP API Server Started" {
    $apiLog = adb logcat -d | Select-String "API Server started" | Select-Object -Last 1
    return $apiLog -ne $null
}

# 5. Test native libraries
Test-Feature "Native Libraries Loaded" {
    $libLog = adb logcat -d | Select-String "libclash.so loaded successfully" | Select-Object -Last 1
    return $libLog -ne $null
}

# Generate report
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Report" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$failed = 0

foreach ($test in $results.Keys | Sort-Object) {
    $result = $results[$test]
    if ($result -eq "PASS") {
        Write-Host "[OK]   $test" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "[FAIL] $test" -ForegroundColor Red
        $failed++
    }
}

Write-Host ""
Write-Host "Total: $($results.Count) tests" -ForegroundColor Cyan
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""

# Next steps
if ($failed -gt 0) {
    Write-Host "NEXT STEPS:" -ForegroundColor Yellow
    Write-Host ""
    
    if ($results["VPN Service Started"] -ne "PASS") {
        Write-Host "1. Start VPN service manually in the app" -ForegroundColor Cyan
    }
    
    if ($results["HTTP API Server Started"] -ne "PASS") {
        Write-Host "2. Check VPN service logs for API server startup" -ForegroundColor Cyan
        Write-Host "   Run: adb logcat -d | Select-String 'ProxyApiServer'" -ForegroundColor Gray
    }
    
    Write-Host ""
}

Write-Host "View detailed logs:" -ForegroundColor Yellow
Write-Host "  adb logcat -d | Select-String 'clash_verge|ProxyApiServer|ClashVpnService'" -ForegroundColor Gray
Write-Host ""
