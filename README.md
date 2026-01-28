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
