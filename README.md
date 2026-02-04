# BearIndonesia 백엔드

백엔드 실행방법    
1. `cd BearIndonesia`
2. `.\gradlew.bat bootRun` 
MacOS에서는 `chmod +x gradlew` `./gradlew bootRun`

# 환경변수 (필수)
JWT_SECRET=임의의_긴_문자열

# 서버에서도 반드시 설정 필요 (없으면 부팅 실패)
# 예) PowerShell
# $env:JWT_SECRET="임의의_긴_문자열"

```

## Docker DB 실행 방법 (서버 기준)

아래 명령은 `BearIndonesia-Back` 루트에서 실행합니다.

DB 시작:
`docker compose -f docker-compose.db.yml up -d`

DB 상태 확인:
`docker compose -f docker-compose.db.yml ps`

DB 중지:
`docker compose -f docker-compose.db.yml down`

주의:
- `down`은 데이터 유지
- `down -v`는 볼륨까지 삭제되어 DB 데이터 초기화

## 실행 흐름 (권장 순서)

1. Docker DB 실행
2. 백엔드 실행 (`BearIndonesia-Back/BearIndonesia`)
3. 파이썬 실행 (`BearIndonesia-Python`)
4. 프론트 실행 (`BearIndonesia-Front/BearIndonesia`)

### 1) DB 실행
`docker compose -f docker-compose.db.yml up -d`

### 2) 백엔드 실행
`.\gradlew.bat bootRun`

### 3) 파이썬 실행
`.\.venv\Scripts\Activate.ps1`
`python -m python_pipeline.crawler.run_backfill`

### 4) 프론트 실행
`npm install`
`npm run dev`

## DB 접속 정보 (현재 서버 설정)

- Host: `127.0.0.1`
- Port: `55432`
- Database: `bear`
- User: `bear`
- SSL mode: `disable`
