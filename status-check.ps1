# eyeO Platform - System Status Check
# Validates all components and provides actionable feedback

param(
    [switch]$Verbose = $false
)

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         eyeO Platform - System Status Check           ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Initialize counters
$issues = @()
$warnings = @()
$successes = @()

# 1. Check Git Repository Status
Write-Host "📦 Git Repository" -ForegroundColor White
Write-Host "─────────────────" -ForegroundColor Gray

try {
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    $status = git status --porcelain 2>$null
    
    if ($branch -eq "main") {
        Write-Host "  ✅ Branch: main" -ForegroundColor Green
        $successes += "Git on main branch"
    } else {
        Write-Host "  ⚠️  Branch: $branch (not main)" -ForegroundColor Yellow
        $warnings += "Working on non-main branch: $branch"
    }
    
    if ($status) {
        $fileCount = ($status | Measure-Object).Count
        Write-Host "  ⚠️  $fileCount uncommitted file(s)" -ForegroundColor Yellow
        $warnings += "Uncommitted changes detected"
    } else {
        Write-Host "  ✅ Working tree clean" -ForegroundColor Green
        $successes += "No uncommitted changes"
    }
} catch {
    Write-Host "  ❌ Git not initialized or error accessing repo" -ForegroundColor Red
    $issues += "Git repository error"
}

Write-Host ""

# 2. Check Docker Status
Write-Host "🐳 Docker Services" -ForegroundColor White
Write-Host "──────────────────" -ForegroundColor Gray

try {
    $containers = docker ps --format "{{.Names}}" 2>$null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ⚠️  Docker not running" -ForegroundColor Yellow
        $warnings += "Docker Desktop not started"
    } else {
        $requiredServices = @(
            "eyeo-identity-db",
            "eyeo-stream-db",
            "identity-service",
            "data-core",
            "stream-processing"
        )
        
        $runningCount = 0
        foreach ($service in $requiredServices) {
            if ($containers -contains $service) {
                Write-Host "  ✅ $service" -ForegroundColor Green
                $runningCount++
            } else {
                Write-Host "  ⭕ $service (not running)" -ForegroundColor Gray
            }
        }
        
        if ($runningCount -eq 0) {
            Write-Host "  ℹ️  No services running (use docker-compose up -d)" -ForegroundColor Cyan
        } elseif ($runningCount -lt $requiredServices.Count) {
            $warnings += "Only $runningCount/$($requiredServices.Count) services running"
        } else {
            $successes += "All Docker services running"
        }
    }
} catch {
    Write-Host "  ⚠️  Docker not available" -ForegroundColor Yellow
    $warnings += "Docker status check failed"
}

Write-Host ""

# 3. Check Port Availability
Write-Host "🔌 Port Status" -ForegroundColor White
Write-Host "───────────────" -ForegroundColor Gray

$requiredPorts = @{
    3306 = "MySQL (system)"
    3307 = "Stream-DB"
    3308 = "Identity-DB"
    5173 = "Frontend Dev Server"
    8080 = "Data-Core Microkernel"
    8081 = "Identity Service"
    8082 = "Stream Processing"
    8090 = "Edge Node"
    8091 = "Middleware"
}

$portsInUse = 0
foreach ($port in $requiredPorts.Keys | Sort-Object) {
    $connection = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    
    if ($connection) {
        $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
        Write-Host "  ✅ Port $port - $($requiredPorts[$port])" -ForegroundColor Green -NoNewline
        
        if ($Verbose -and $process) {
            Write-Host " ($($process.ProcessName))" -ForegroundColor Gray
        } else {
            Write-Host ""
        }
        $portsInUse++
    } else {
        Write-Host "  ⭕ Port $port - $($requiredPorts[$port]) (available)" -ForegroundColor Gray
    }
}

if ($portsInUse -eq 0) {
    Write-Host "  ℹ️  All ports available (ready for startup)" -ForegroundColor Cyan
}

Write-Host ""

# 4. Check Configuration Files
Write-Host "⚙️  Configuration Files" -ForegroundColor White
Write-Host "───────────────────────" -ForegroundColor Gray

$configFiles = @(
    @{ Path = "docker-compose.dev.yml"; Required = $true },
    @{ Path = "frontend/.env.development"; Required = $false },
    @{ Path = "frontend/vite.config.ts"; Required = $true },
    @{ Path = ".gitignore"; Required = $true }
)

foreach ($config in $configFiles) {
    if (Test-Path $config.Path) {
        Write-Host "  ✅ $($config.Path)" -ForegroundColor Green
        $successes += "Config file exists: $($config.Path)"
    } else {
        if ($config.Required) {
            Write-Host "  ❌ $($config.Path) (REQUIRED)" -ForegroundColor Red
            $issues += "Missing required config: $($config.Path)"
        } else {
            Write-Host "  ⚠️  $($config.Path) (optional)" -ForegroundColor Yellow
            $warnings += "Missing optional config: $($config.Path)"
        }
    }
}

Write-Host ""

# 5. Check for Sensitive Files
Write-Host "🔒 Security Audit" -ForegroundColor White
Write-Host "─────────────────" -ForegroundColor Gray

$sensitivePatterns = @(
    "CHECKPOINT*.md",
    "DEPLOYMENT_TEST*.md",
    "*GUIDE*.md"
)

$foundSensitive = $false
foreach ($pattern in $sensitivePatterns) {
    $files = git ls-files $pattern 2>$null
    
    if ($files) {
        foreach ($file in $files) {
            Write-Host "  ⚠️  $file (tracked in git)" -ForegroundColor Yellow
            $warnings += "Sensitive file tracked: $file"
            $foundSensitive = $true
        }
    }
}

if (-not $foundSensitive) {
    Write-Host "  ✅ No sensitive documentation in git" -ForegroundColor Green
    $successes += "Security check passed"
}

Write-Host ""

# 6. Summary and Recommendations
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan

Write-Host "✅ Successes: $($successes.Count)" -ForegroundColor Green
Write-Host "⚠️  Warnings:  $($warnings.Count)" -ForegroundColor Yellow
Write-Host "❌ Issues:    $($issues.Count)" -ForegroundColor Red
Write-Host ""

if ($issues.Count -gt 0) {
    Write-Host "🚨 CRITICAL ISSUES:" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "   • $issue" -ForegroundColor Red
    }
    Write-Host ""
}

if ($warnings.Count -gt 0 -and $Verbose) {
    Write-Host "⚠️  WARNINGS:" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "   • $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Recommendations
Write-Host "📋 NEXT STEPS:" -ForegroundColor Cyan

if ($warnings -match "Docker Desktop not started") {
    Write-Host "   1. Start Docker Desktop (optional for local development)" -ForegroundColor White
}

if ($warnings -contains "Uncommitted changes detected") {
    Write-Host "   2. Review and commit changes: git status" -ForegroundColor White
}

if ($portsInUse -eq 0) {
    Write-Host "   3. Start development: docker-compose -f docker-compose.dev.yml up -d" -ForegroundColor White
}

Write-Host ""
Write-Host "Run './status-check.ps1 -Verbose' for detailed information" -ForegroundColor Gray
Write-Host ""
