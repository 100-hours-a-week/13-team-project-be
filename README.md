<div align="center">

<img src="https://github.com/user-attachments/assets/3c11cba2-5973-4be5-826c-fc4bd9e59d82" alt="모여밥 - 모두의 취향을 반영한 식당 추천 서비스" width="900" />

# 모여밥 (Moyeobab)


### 모두의 취향을 반영한 식당 추천 서비스

</div>

---

## 🍽️ 모여밥이 해결하는 문제

> *"오늘 점심 뭐 먹지?"*
> *"민지는 알레르기 있고, 준호는 매운 거 못 먹고, 누구는 어제 먹은 음식이랑 겹쳐서 싫고..."*

**모여밥은 모임 구성원의 취향·알레르기·선호 카테고리를 모두 반영해 식당을 추천하고,
채팅·투표·정산까지 한 번에 끝내는 서비스입니다.**

---

## 🧭 사용자 흐름 (User Journey)

### 1. 모임을 만들고, 모두의 취향을 모으자!

<img src="https://github.com/user-attachments/assets/2ddebeeb-d133-4eab-b208-cc3a6c28ed80"  alt="알레르기 + 선호 카테고리 + 모임 정보" width="800" />

각 멤버의 **알레르기 그룹**, **선호 카테고리**, **비선호 카테고리**가 모임 정보와 결합되어
**음식점 투표 후보**가 자동으로 생성됩니다.

### 2. 투표로 식당을 결정!

<img src="https://github.com/user-attachments/assets/146b2dfa-564c-4139-9782-329e8a23a686" alt="투표를 통한 최종 식당 선정" width="800" />

AI가 추천한 후보 중에서 **TOP 3 투표**로 최종 식당을 결정합니다.

### 3. 채팅하고, 먹고, 영수증으로 정산까지!

<img src="https://github.com/user-attachments/assets/e8d5f10e-5aab-46c9-8243-60fe53c0f4d1" alt="채팅 + OCR 영수증 정산" width="800" />

모임원과 실시간 채팅으로 약속을 잡고, 식사 후에 **영수증을 찍고 업로드하면 정산을** 도와줍니다.

---

## 🎯 핵심 기능

| 기능 | 설명 |
|---|---|
| 🍱 **AI 식당 추천** | 멤버 알레르기·선호/비선호 카테고리 + 모임 위치·시간을 반영한 RAG 기반 추천 |
| 💬 **단체 채팅** | WebSocket(STOMP) 기반 실시간 메시지, 이미지 업로드, 읽음 처리 |
| 🤖 **챗봇 (AI Assistant)** | "근처 매운 거 잘하는 곳" 같은 자연어 질문 → SSE 스트리밍 응답 |
| 🗳️ **식당 투표** | 후보 중 TOP 3 투표로 최종 결정, 쿠폰 사용 흐름 |
| 🧾 **영수증 OCR 정산** | 영수증 사진 → 항목/금액 자동 파싱 → 멤버별 분담 계산 |
| ⚡ **퀵모임** | 즉석 모임 생성 (간소화된 모임 흐름) |
| 🔔 **푸시 알림 (FCM)** | 채팅·투표·정산·모임 참여 이벤트를 푸시로 전달 |
| 🎁 **쿠폰 / 이벤트** | 모임/정산과 연동된 쿠폰 사용 |
| ⭐ **리뷰** | 모임 종료 후 식당·멤버 리뷰 |

---

## 👥 팀원 소개

<img src="https://github.com/user-attachments/assets/6f9ec754-cd96-4b5a-8d75-e750a693ce91" alt="팀원 소개" width="700" />

| 이름 | 역할 | 담당 |
|---|---|---|
| **veyla.kim** (김다미) | 팀장 / AI | 음식점 데이터 크롤링, 음식점 추천 모델, RAG 시스템 |
| **min.kim** (김민) | AI | OCR 정산 기능 |
| **ellim.lee** (이기용) | Cloud | 인프라 설계 · 구축 |
| **felix.son** (손성민) | Cloud | 모니터링 및 알림 시스템, 장애 대응 체계 |
| **lucas.kim** (김동혁) | Fullstack | 로그인/회원가입, 프로필, 모임/인가, 단체 채팅, 챗봇 연동, 푸시알림, 성능 개선 |
| **hazel.lee** (이홍진) | Fullstack | 모임, 투표, 정산, 퀵모임, 이벤트, 리뷰 기능 구현 |

---

## 🛠️ 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square&logo=rabbitmq&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)

### Frontend
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite&logoColor=white)

### Infra & Ops
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat-square&logo=amazons3&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_FCM-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-000000?style=flat-square&logo=opentelemetry&logoColor=white)

**기타**: Spring Security · WebSocket(STOMP) · Resilience4j (Circuit Breaker / Bulkhead / Retry) · JJWT · Micrometer


---

## 📂 디렉토리 구조

```
Moyeobab/
├── be/                              # Spring Boot 백엔드 (Java 21)
│   ├── src/main/java/com/matchimban/matchimban_api/
│   │   ├── auth/                    # JWT 인증
│   │   ├── chat/                    # 실시간 채팅 (WebSocket/STOMP)
│   │   ├── meeting/                 # 모임
│   │   ├── member/                  # 회원 / 프로필
│   │   ├── notification/            # 알림 
│   │   ├── ragchat/                 # AI 챗봇 
│   │   ├── restaurant/              # 식당
│   │   ├── settlement/              # 정산 / OCR / S3
│   │   ├── vote/                    # 투표
│   │   └── global/                  # 공통 (config / security / aws)
│   ├── src/main/resources/db/migration/   # Flyway V1..V19
│   └── docker-compose.yml           # postgres + redis
│
├── fe/                              # React + Vite 프론트엔드 
│   └── src/

```



---

<div align="center">

**모여밥** · *모두의 취향을 반영한 식당 추천 서비스*

</div>
