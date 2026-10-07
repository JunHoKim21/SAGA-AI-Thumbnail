# Blogger Publisher 스킬 이전 가이드 (Manual)

이 폴더는 Google Gemini(Antigravity) 에이전트가 블로그스팟(Blogger)에 자동으로 글을 퍼블리싱할 수 있게 해주는 커스텀 스킬 폴더입니다.
현재 구글 인증 토큰(token.pickle)이 포함되어 있어, 다른 컴퓨터에 복사하면 별도의 로그인 없이 바로 사용할 수 있습니다.

## 🚀 다른 컴퓨터에 스킬 적용하는 방법

### 1단계: 스킬 폴더 위치시키기
압축을 푼 이 logger-publisher 폴더 전체를 아래의 경로에 복사해서 붙여넣습니다.
> **경로**: C:\Users\사용자이름\.gemini\config\skills\
(만약 해당 경로에 skills 폴더가 없다면 직접 만들어주시면 됩니다.)

### 2단계: 필수 파이썬 패키지 설치
이 스킬은 파이썬(Python)과 구글 API 라이브러리를 사용합니다. 
키보드의 Windows 키를 누르고 cmd 또는 PowerShell을 검색해서 연 다음, 아래 명령어를 복사해서 붙여넣고 엔터를 치세요.

\\\powershell
pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib
\\\

### 3단계: AI에게 명령하기
이제 세팅이 완료되었습니다!
평소처럼 AI 챗봇을 열고 이렇게 명령해 보세요.
> "방금 우리가 이야기한 내용을 정리해서 블로그에 올려줘"
> "이 코드를 설명하는 기술 블로그 포스팅을 작성해서 발행해줘"

AI가 스스로 이 스킬을 인식하고 블로그에 글을 올려줄 것입니다! 🎉
