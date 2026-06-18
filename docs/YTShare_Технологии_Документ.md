# YTShare — Преглед на използваните технологии

**Проект:** YTShare  
**Автор:** Ивайло Илиев  
**Дата:** Март 2026  

---

## Съдържание

1. [Въведение](#1-въведение)
2. [Kotlin](#2-kotlin)
3. [Gradle](#3-gradle)
4. [Jetpack Compose](#4-jetpack-compose)
5. [Material 3 (Material You)](#5-material-3-material-you)
6. [C#](#6-c)
7. [Java](#7-java)
8. [Maven](#8-maven)
9. [Spring Boot](#9-spring-boot)
10. [Lombok](#10-lombok)
11. [MapStruct](#11-mapstruct)
12. [Firebase Authentication](#12-firebase-authentication)
13. [PostgreSQL](#13-postgresql)
14. [Liquibase](#14-liquibase)
15. [TypeScript](#15-typescript)
16. [Angular](#16-angular)
17. [AWS (Amazon Web Services)](#17-aws-amazon-web-services)
18. [Заключение](#18-заключение)

---

## 1. Въведение

YTShare е мултиплатформен софтуерен проект, чиято основна цел е да даде възможност на потребителите да споделят YouTube видеоклипове от мобилното си Android устройство към настолен компютър, намиращ се в същата локална мрежа (LAN). Концепцията е проста, но изключително полезна: потребителят открива интересно видео на телефона си и с едно натискане го изпраща към компютъра, където то се отваря автоматично в браузъра.

Отвъд тази основна функционалност проектът се развива и в по-мащабна посока — разработва се **backend** сървър и **уеб приложение (secondary frontend)**, които осигуряват социални функции: управление на потребителски профили, система за приятелства, чат съобщения, запазване на споделени видеоклипове и потребителски предпочитания.

Архитектурата на проекта обхваща четири основни компонента:

- **YTShare.Android** — мобилното Android приложение (Kotlin + Jetpack Compose)
- **YTShare.Host** — десктоп услугата за Windows (C# / ASP.NET Core Web API)
- **backend** — централен сървър за социални функции (Java / Spring Boot)
- **frontend** — уеб интерфейс за социални функции (TypeScript / Angular)

За реализацията си проектът разчита на **15 основни технологии**, всяка от които играе своя уникална и незаменима роля. В настоящия документ ще разгледаме подробно всяка от тях — какво представлява, защо е избрана и какъв е нейният конкретен принос за YTShare.

---

## 2. Kotlin

### 2.1. Какво е Kotlin?

Kotlin е модерен, статично типизиран програмен език, разработен от JetBrains. От 2019 г. Google обяви Kotlin като **предпочитания език за Android разработка** (Kotlin-first approach), което означава, че всички нови Android API-та и библиотеки са проектирани с приоритет за Kotlin.

Kotlin работи върху Java Virtual Machine (JVM), което го прави напълно съвместим с Java екосистемата, но предлага значително по-кратък и по-изразителен синтаксис. Езикът въвежда нулева безопасност (null safety) на ниво тип система, data класове, extension функции, корутини за асинхронно програмиране и множество други модерни концепции.

### 2.2. Роля в YTShare

В проекта YTShare, Kotlin е езикът, на който е написано цялото **Android мобилно приложение** (`YTShare.Android`). Конкретно:

- **`MainActivityCompose.kt`** — главният Activity, който инициализира Jetpack Compose UI-то, управлява навигацията между екраните (Home, History, Settings), обработва споделени линкове чрез `Intent.ACTION_SEND` и координира всички помощни класове.
- **`HomeScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`** — Composable екрани, изградени изцяло на Kotlin, определящи визуалното представяне и потребителското взаимодействие.
- **`NSDHelper.kt`** — помощен клас за Network Service Discovery (NSD), който сканира локалната мрежа за активни YTShare Host устройства. Тази функционалност е ключова за автоматичното откриване на компютри.
- **`DBHelper.kt`** — локален SQLite помощник за съхраняване на историята на споделените линкове.
- **`SharedPrefHelper.kt`** — управление на потребителските предпочитания чрез SharedPreferences.

Kotlin е от ключово значение за проекта, тъй като осигурява **чист, поддържаем код** с минимално количество boilerplate. Null safety механизмът предотвратява цял клас от потенциални runtime грешки, а корутините (използвани в Compose екраните чрез `LaunchedEffect`) позволяват асинхронни операции без блокиране на UI нишката.

---

## 3. Gradle

### 3.1. Какво е Gradle?

Gradle е мощна система за автоматизация на билдове (build automation tool), която комбинира най-доброто от Apache Ant и Apache Maven. За разлика от XML-базирания подход на Maven, Gradle използва Kotlin DSL или Groovy DSL за описване на билд конфигурацията, което позволява по-голяма гъвкавост и програмируемост.

Gradle е **стандартната билд система за Android проекти** и е дълбоко интегрирана с Android Studio. Тя управлява зависимостите, компилацията, пакетирането на APK/AAB файлове, изпълнението на тестове и много други.

### 3.2. Роля в YTShare

В YTShare, Gradle управлява целия **билд процес на Android приложението**. Проектът използва **Kotlin DSL** (`build.gradle.kts`) за конфигурация, което е по-модерният подход. Конкретно:

- **Управление на зависимости** — чрез Version Catalog (`libs.*`), Gradle дефинира и управлява всички библиотеки, които приложението използва: Compose BOM, Material3, Navigation Compose, Volley за мрежови заявки, Coil за зареждане на изображения, ZXing за QR кодове и други.
- **Конфигурация на билда** — `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24` —определят целевите Android версии. Тази конфигурация осигурява съвместимост с огромен брой устройства (от Android 7.0 Nougat нагоре), като същевременно използва най-новите API-та.
- **Compose Compiler Plugin** — Gradle интегрира специалния Compose compiler plugin (`libs.plugins.compose.compiler`), който е необходим за трансформирането на `@Composable` функции по време на компилация.
- **ProGuard конфигурация** — за release билдовете е конфигуриана оптимизация чрез ProGuard.

Без Gradle, координирането на десетките зависимости и сложната билд конфигурация на Android проекта би било практически невъзможно. Gradle осигурява **повторяемост, автоматизация и надеждност** на целия билд процес.

---

## 4. Jetpack Compose

### 4.1. Какво е Jetpack Compose?

Jetpack Compose е **модерен декларативен UI toolkit** на Google за създаване на потребителски интерфейси в Android. За разлика от традиционния императивен подход с XML layouts и `findViewById`, Compose позволява на разработчиците да описват UI-то като функции, които се преизчисляват автоматично при промяна на състоянието.

Compose е вдъхновен от концепциите на React и Flutter и представлява фундаментална промяна в начина, по който се разработват Android интерфейси. Работи изцяло на Kotlin и елиминира необходимостта от XML файлове за описание на layouts.

### 4.2. Роля в YTShare

YTShare Android приложението използва Jetpack Compose като **основен UI framework**. Целият потребителски интерфейс е изграден чрез Composable функции:

- **`MainScreen()`** — главният Composable, който съдържа `Scaffold` с `NavigationBar` (долна навигация) и `NavHost` за навигация между екраните. Тук се управлява състоянието на приложението чрез `remember` и `mutableStateOf`.
- **`HomeScreen`** — екранът, от който потребителят изпраща YouTube линкове. Използва Compose за динамично показване на IP адреса, запазения линк и бутони за действие.
- **`HistoryScreen`** — показва списък с предишно споделени видеоклипове. Compose прави лесно динамичното актуализиране на списъка, когато потребителят изтрива записи.
- **`SettingsScreen`** — екран за настройки, където потребителят избира хост устройство от списъка с открити компютри в локалната мрежа и конфигурира предпочитания като tracking.

Съществено предимство на Compose в този проект е **реактивността** — когато се промени стойността на `hosts` (списъкът с открити компютри в мрежата), `ipAddress`, `savedLink` или `isTracking`, съответните UI елементи се преизграждат автоматично, без нужда от ръчно обновяване. Използването на `LaunchedEffect` позволява изпълнение на странични ефекти (като презареждане на видеоклипове от базата данни) при навигация между екраните.

Навигацията между екраните е реализирана чрез **Navigation Compose** (`rememberNavController`, `NavHost`, `composable()`), което е стандартният подход за навигация в Compose приложения.

---

## 5. Material 3 (Material You)

### 5.1. Какво е Material 3?

Material 3 (известен още като Material You) е **най-новата версия на дизайн системата на Google**. Тя въвежда концепцията за динамичен цвят (Dynamic Color), при който цветовата палитра на приложението може да се адаптира автоматично към тапета на потребителя (на Android 12+). Material 3 предлага обновени компоненти с по-обли ъгли, по-изразителна типография и подобрена достъпност.

### 5.2. Роля в YTShare

В YTShare, Material 3 е дизайн системата, която определя **визуалната идентичност** на Android приложението:

- **`NavigationBar`** и **`NavigationBarItem`** — долната навигационна лента използва Material 3 компоненти с персонализирани цветове (`containerColor = Color.Red`, `contentColor = Color.White`), създавайки разпознаваема визуална идентичност, асоциирана с YouTube тематиката.
- **`Scaffold`** — основният layout компонент на Material 3, който управлява позиционирането на навигационната лента и съдържанието.
- **`Icon`**, **`Text`** — базови Material 3 компоненти, използвани навсякъде в приложението.
- **Material Icons** (`Icons.Filled.Home`, `Icons.Filled.History`, `Icons.Filled.Settings`) — иконографията на приложението използва стандартните Material Design икони.
- **`YTShareTheme`** — персонализирана Material 3 тема, дефинирана в пакета `ui/theme`, която определя цветовата палитра, типографията и формите.

Material 3 осигурява на YTShare **професионален и модерен външен вид**, съвместим с най-новите Android дизайн стандарти, без необходимост от ръчно проектиране на всеки UI компонент.

---

## 6. C#

### 6.1. Какво е C#?

C# (произнася се „Си Шарп") е **обектно-ориентиран език за програмиране**, създаден от Microsoft като част от платформата .NET. Езикът съчетава мощта на C++ с продуктивността на по-високоуровнените езици. C# е един от най-популярните езици в света и се използва за разработка на десктоп приложения, уеб услуги, игри (чрез Unity) и много други.

### 6.2. Роля в YTShare

C# е езикът, на който е написан компонентът **YTShare.Host** — **десктоп услугата**, която работи на настолния компютър на потребителя и приема споделените YouTube линкове. Конкретно:

- **`Program.cs`** — основният файл на услугата, изграден върху ASP.NET Core Web API. Той дефинира минимален HTTP сървър, който слуша на порт 7296 и обработва GET заявки по endpoint `/Share`. Когато получи линк, сървърът го отваря автоматично в уеб браузъра на компютъра чрез `Process.Start()`.
- **Bonjour/mDNS интеграция** — C# кодът регистрира услугата като mDNS/Bonjour сервиз (`_http._tcp.`), което позволява на Android приложението автоматично да я открие в локалната мрежа, без потребителят да въвежда IP адреси ръчно.
- **Windows Service** — благодарение на `UseWindowsService()`, приложението може да работи като **фонова Windows услуга**, стартираща автоматично при зареждане на операционната система.

Проектът използва **.NET 8.0** (`net8.0` target framework) и пакета `Microsoft.Extensions.Hosting.WindowsServices` за интеграция с Windows Service инфраструктурата.

C# е избран за Host компонента заради изключителната му интеграция с Windows операционната система, вградената поддръжка за Windows Services и лесното използване на COM interop за достъп до Bonjour API на Apple.

---

## 7. Java

### 7.1. Какво е Java?

Java е един от **най-разпространените програмни езици в света**, създаден от Sun Microsystems (понастоящем Oracle) през 1995 г. Принципът „Write Once, Run Anywhere" и стабилната Java Virtual Machine (JVM) го правят предпочитан избор за enterprise приложения, уеб сървъри, микросървиси и Android разработка.

Java екосистемата е огромна — включва хиляди библиотеки, frameworks и инструменти, натрупани в продължение на три десетилетия.

### 7.2. Роля в YTShare

Java е езикът, на който е написан **целият backend** на YTShare. Проектът използва **Java 21** (LTS — Long Term Support), което е най-новата дългосрочно поддържана версия. Бекенд приложението е организирано в следните пакети:

- **`user`** — управление на потребителски профили (User entity, UserController, UserService, UserRepository, UserMapper и DTO-та).
- **`friends`** — система за приятелства (Friendship entity с различни статуси, контролер за изпращане/приемане/отказване на покани).
- **`chat`** / **`message`** — чат функционалност: чатове с множество участници, съобщения със статуси.
- **`video`** — управление на споделени видеоклипове (заглавие, описание, URL, thumbnail URL).
- **`device`** — регистрация и управление на устройства (hostname, IP адрес, порт, последно свързване).
- **`security`** — Firebase Authentication интеграция (филтри, конфигурация, помощни класове).
- **`common`** — споделени базови класове (BaseEntity с UUID id, createdAt, updatedAt).

Java е избрана за бекенда заради своята **стабилност, зрялост и богата екосистема** от enterprise инструменти. В комбинация със Spring Boot, тя осигурява надеждна основа за изграждане на REST API сървър, способен да обслужва множество потребители едновременно.

---

## 8. Maven

### 8.1. Какво е Maven?

Apache Maven е инструмент за **управление на проекти и автоматизация на билдове** в Java екосистемата. Maven използва **Project Object Model (POM)** — XML файл (`pom.xml`), който описва структурата на проекта, неговите зависимости, плъгини и билд настройки. Maven автоматично изтегля зависимости от централизирани хранилища (Maven Central), управлява транзитивните зависимости и стандартизира жизнения цикъл на билда.

### 8.2. Роля в YTShare

Maven е билд системата на **backend** компонента на YTShare. Файлът `pom.xml` определя:

- **Parent POM** — `spring-boot-starter-parent` версия 4.0.1, който предоставя стандартизирани настройки за Spring Boot проекти, включително managed версии на стотици зависимости.
- **Зависимости** — Maven управлява всички библиотеки, от които се нуждае бекенда:
  - `spring-boot-starter-data-jpa` — за работа с базата данни
  - `spring-boot-starter-webmvc` — за REST API
  - `spring-boot-starter-liquibase` — за миграции на базата
  - `spring-boot-starter-security` — за защита на endpoints
  - `postgresql` — JDBC драйвер за PostgreSQL
  - `lombok`, `mapstruct` — за продуктивност при писане на код
  - `firebase-admin` — за верификация на Firebase токени
- **Annotation Processors** — Maven compiler plugin е конфигуриран с **специфичен ред на annotation processors**: Spring Boot Configuration Processor → Lombok → MapStruct. Този ред е критичен, защото MapStruct трябва да вижда getter/setter методите, генерирани от Lombok.
- **Hibernate Maven Plugin** — конфигуриран за enhanced association management.
- **Docker integration** — `spring-boot-docker-compose` зависимостта позволява автоматично стартиране на Docker Compose контейнери при development.
- **Dockerfile** — Maven wrapper (`mvnw`) се използва вътре в Docker за билдване на приложението: `./mvnw clean package -DskipTests`.

Maven осигурява **повторяемост и предвидимост** на билд процеса — независимо дали разработчикът билдва на локалната си машина или в Docker контейнер, резултатът е идентичен.

---

## 9. Spring Boot

### 9.1. Какво е Spring Boot?

Spring Boot е **framework за бързо разработване на production-ready приложения** на базата на Spring Framework. Той елиминира голяма част от комплексната конфигурация, характерна за традиционния Spring, чрез принципа на „convention over configuration" и автоматична конфигурация (auto-configuration).

Spring Boot предоставя вграден уеб сървър (Tomcat), управление на зависимости, профили за различни среди, health checks, metrics и много други production-ready функции „от кутията".

### 9.2. Роля в YTShare

Spring Boot е **гръбнакът на backend сървъра** на YTShare. Проектът използва **Spring Boot 4.0.1** — най-новата версия. Ето как различните Spring Boot стартери се използват:

- **`spring-boot-starter-webmvc`** — осигурява REST API функционалност. Контролерите (`UserController`, `VideoController`, `FriendshipController`, `DeviceController`) дефинират HTTP endpoints за CRUD операции, като Spring Boot автоматично сериализира/десериализира JSON.
- **`spring-boot-starter-data-jpa`** — интеграция с JPA (Hibernate). Repository интерфейси (`UserRepository`, `VideoRepository`, `FriendshipRepository`, `DeviceRepository`) наследяват `JpaRepository` и получават CRUD методи автоматично, без да пишат SQL.
- **`spring-boot-starter-security`** — Spring Security е интегрирана с Firebase Authentication чрез персонализиран `FirebaseTokenFilter`, който прихваща всяка HTTP заявка, извлича Bearer токена от Authorization header-а и го верифицира чрез Firebase Admin SDK. Сесиите са **stateless** (няма session cookie), а всяка заявка се автентицира чрез токен.
- **`spring-boot-starter-liquibase`** — автоматично стартиране на Liquibase миграции при зареждане на приложението.
- **`spring-boot-devtools`** — автоматичен restart на приложението при промяна на код по време на разработка.
- **`spring-boot-docker-compose`** — автоматично стартиране на PostgreSQL и други контейнери при development.

Spring Boot конфигурацията (`application.properties`) използва **environment variables** (`${DB_HOST:localhost}`) за гъвкава конфигурация в различни среди — development, staging, production.

Благодарение на Spring Boot, YTShare backend стартира за секунди, има вградена поддръжка за Docker, и предоставя цялостна REST API инфраструктура без нужда от допълнителна конфигурация на уеб сървър.

---

## 10. Lombok

### 10.1. Какво е Lombok?

Project Lombok е **Java библиотека**, която автоматично генерира повтарящ се код (boilerplate) чрез анотации. Вместо ръчно да пишете getters, setters, constructors, `toString()`, `hashCode()`, `equals()` и други стандартни методи, Lombok ги генерира по време на компилация от анотации като `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` и `@RequiredArgsConstructor`.

### 10.2. Роля в YTShare

Lombok е използван **масово в целия backend** на YTShare за елиминиране на boilerplate код:

- **`BaseEntity.java`** — базовият entity клас използва `@Getter`, `@Setter`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`. Без Lombok, този клас от 38 реда би бил поне 100+ реда с ръчно написани методи.
- **`User.java`** — наследява BaseEntity и добавя своите полета, също с пълен набор Lombok анотации. Полета като `firebaseUid`, `email`, `firstName`, `lastName` автоматично получават getter и setter методи.
- **`SecurityConfig.java`** — използва `@RequiredArgsConstructor`, за да генерира конструктор, инжектиращ `FirebaseTokenFilter` автоматично (constructor injection — най-препоръчваният подход в Spring).
- **`FirebaseTokenFilter.java`** — освен `@RequiredArgsConstructor`, използва и `@Slf4j`, който автоматично създава logger инстанция (`log.debug()`, `log.warn()`).
- **`BackendApplication.java`** — дори главният клас използва `@NoArgsConstructor(access = PRIVATE)` за спазване на добри практики.

В билд конфигурацията (`pom.xml`) **редът на annotation processors е критичен**: Lombok трябва да се изпълни **преди MapStruct**, за да може MapStruct да визуализира getter/setter методите, генерирани от Lombok. Тази конфигурация е установена в `maven-compiler-plugin`.

Lombok значително повишава **четимостта и поддръжката** на кода — entity класовете се фокусират върху дефинирането на полета и бизнес логика, вместо върху стотици редове инфраструктурен код.

---

## 11. MapStruct

### 11.1. Какво е MapStruct?

MapStruct е **code generation framework** за Java, който автоматично генерира тип-безопасни mapper-и между Java beans (обикновено между Entity и DTO обекти). За разлика от runtime mapping библиотеки като ModelMapper или Dozer, MapStruct генерира кода по време на **компилация**, което означава нулев runtime overhead и пълна тип-безопасност.

### 11.2. Роля в YTShare

MapStruct играе критична роля в YTShare backend, като осигурява **чисто разделение между domain entities и DTO (Data Transfer Objects)**:

- **`UserMapper`** — трансформира `User` entity в `UserOutputDto` (за отговори на API) и `UserInputDto` в `User` entity (за създаване на нови потребители). Използва `UserPreferencesMapper` като вложен мап.

- **`DeviceMapper`** — по-сложен mapper с explicit mapping анотации:
  ```java
  @Mapping(target = "userPreferencesId", source = "userPreferences.id")
  DeviceOutputDto toOutputDto(final Device device);
  ```
  Това автоматично извлича `id` от вложения `userPreferences` обект и го поставя като flat поле в DTO-то. Включва и `updateEntity()` метод за частично обновяване на съществуващ entity.

- **`FriendshipMapper`** — работи с вложени entities (`User` обекти), като маппва `userId` и `friendId` от DTO-то към съответните `user.id` и `friend.id` в entity-то. Включва и `updateStatus()` метод за обновяване само на статуса на приятелството.

- **`VideoMapper`** — трансформира между `Video` entity и съответните DTO обекти.

В `pom.xml` е конфигуриран `componentModel = spring` чрез compiler argument:
```
-Amapstruct.defaultComponentModel=spring
```
Това означава, че генерираните mapper имплементации автоматично стават **Spring beans** и могат да бъдат инжектирани чрез Dependency Injection.

MapStruct гарантира, че **вътрешните domain обекти никога не се експонират директно** чрез API-то — API потребителите виждат само DTO обекти с контролирано съдържание. Това е фундаментална архитектурна практика за сигурност и поддръжка.

---

## 12. Firebase Authentication

### 12.1. Какво е Firebase Authentication?

Firebase Authentication е **услуга за управление на идентичности**, предоставена от Google като част от Firebase платформата. Тя предлага готови решения за email/password автентикация, социална автентикация (Google, Facebook, GitHub), телефонна автентикация и др. Firebase Authentication използва JSON Web Tokens (JWT) за представяне на потребителски сесии и работи безсериозно (stateless) — всеки токен съдържа цялата необходима информация за верификация.

### 12.2. Роля в YTShare

Firebase Authentication е **единствената система за автентикация** в YTShare, използвана както в Angular frontend-а, така и в Spring Boot backend-а:

**На Frontend (Angular):**
- `AuthService` инициализира Firebase чрез `initializeApp(environment.firebase)` и `getAuth()`.
- Поддържа **регистрация** (`createUserWithEmailAndPassword`), **вход** (`signInWithEmailAndPassword`) и **изход** (`signOut`).
- Следи състоянието на автентикация реактивно чрез `onAuthStateChanged` и Angular Signals (`signal`, `computed`).
- `authInterceptor` автоматично прикачва Firebase ID токена като `Authorization: Bearer <token>` header към всяка HTTP заявка към backend API-то.
- `authGuard` и `guestGuard` контролират достъпа до маршрутите — неавтентицирани потребители се пренасочват към `/login`, а автентицираните не могат да достъпят login/register страниците.

**На Backend (Spring Boot):**
- `FirebaseConfig` инициализира Firebase Admin SDK от JSON credentials, подадени чрез environment variable `FIREBASE_CREDENTIALS_JSON`.
- `FirebaseTokenFilter` прихваща всяка HTTP заявка, извлича Bearer токена и го верифицира чрез `firebaseAuth.verifyIdToken(token)`. При успешна верификация създава `FirebaseAuthenticationToken` и го поставя в Spring Security Context.
- `SecurityConfig` конфигурира stateless сесии (`SessionCreationPolicy.STATELESS`) и изисква автентикация за всички endpoints освен `/api/public/**`.
- `SecurityUtils` предоставя помощни методи за извличане на Firebase UID от Security Context.

**В базата данни:**
- Таблица `users` съдържа колона `firebase_uid` с unique constraint, която свързва Firebase потребителя с вътрешния потребителски профил.

Firebase Authentication значително опростява разработката — YTShare не трябва да имплементира собствена система за хеширане на пароли, управление на сесии, reset на пароли и всички свързани с тях потенциални уязвимости. Платформата е безплатна за до 50,000 месечни активни потребители.

---

## 13. PostgreSQL

### 13.1. Какво е PostgreSQL?

PostgreSQL е **мощна, с отворен код обектно-релационна система за управление на бази данни** (ORDBMS). Известна е със своята надеждност, стабилност и пълнота на функционалностите. PostgreSQL поддържа ACID транзакции, сложни заявки, JSON типове данни, пълнотекстово търсене, геопространствени данни (PostGIS) и множество разширения. Тя е предпочитана в enterprise среди и cloud инфраструктури.

### 13.2. Роля в YTShare

PostgreSQL е **единствената релационна база данни** на YTShare backend и съхранява всички потребителски данни и социални връзки. Структурата на базата включва следните таблици:

- **`users`** — потребителски профили (`id` UUID, `firebase_uid`, `email`, `first_name`, `last_name`, `created_at`, `updated_at`). Email-ът и Firebase UID са уникални.
- **`friendships`** — връзки между потребители (`user_id`, `friend_id`, `status`), с foreign keys към таблицата `users`.
- **`chats`** — чат стаи с timestamps.
- **`chat_participants`** — Many-to-Many връзка между `chats` и `users`.
- **`messages`** — съобщения (`content`, `status`, `chat_id`, `sender_id`).
- **`videos`** — споделени видеоклипове (`title`, `description`, `url`, `thumbnail_url`).
- **`devices`** — регистрирани устройства (`host_name`, `ip_address`, `port`, `last_connected_to`).
- **`user_prefs`** — потребителски предпочитания (`dark_mode`, `notifications`, `tracking`).

Базата данни е конфигурирана чрез Docker Compose с **PostgreSQL 16 Alpine** image, което осигурява лека и бърза инстанция. Health check-ът (`pg_isready`) гарантира, че backend приложението стартира чак след като базата е напълно готова.

В `application.properties`, Hibernate е конфигуриран с `ddl-auto=validate` — той само **проверява** дали схемата на базата отговаря на entity класовете, но **не я модифицира** автоматично. Управлението на схемата се извършва изцяло от Liquibase. Допълнително са конфигурирани batch оптимизации (`batch_size=20`, `order_inserts=true`, `order_updates=true`) за подобрена производителност.

PostgreSQL е избрана заради нейната **надеждност, мащабируемост и отлична поддръжка на UUID** като тип на primary key, което е от съществено значение за разпределени системи.

---

## 14. Liquibase

### 14.1. Какво е Liquibase?

Liquibase е **инструмент за управление на версии на базата данни** (database schema migration tool). Подобно на Git за изходен код, Liquibase проследява всяка промяна в схемата на базата данни чрез „change logs" — файлове, описващи промените по декларативен начин (YAML, XML, JSON или SQL). Всяка промяна (changeSet) има уникален идентификатор и автор, и се изпълнява **точно веднъж**.

### 14.2. Роля в YTShare

Liquibase управлява **цялата еволюция на схемата** на PostgreSQL базата данни в YTShare. Конфигурацията включва:

- **Master changelog** (`db.changelog-master.yaml`) — главният файл, който включва отделните changelog файлове по хронологичен ред.
- **`13-01-changelog.yaml`** — първият changelog с 14 changeSets, създаващ основните таблици:
  - Таблици `users`, `friendships`, `chats`, `chat_participants`, `messages`, `videos`
  - Unique constraints за `email` и `firebase_uid`
  - Foreign key constraints свързващи `friendships` → `users`, `messages` → `chats`/`users`, `chat_participants` → `chats`/`users`
- **`26-01-changelog.xml`** — втори changelog с 5 changeSets, добавящ:
  - Таблица `devices` (за регистрирани устройства)
  - Таблица `user_prefs` (потребителски предпочитания)
  - Unique constraint за `user_id` в `user_prefs`
  - Foreign keys свързващи `devices` → `user_prefs` и `user_prefs` → `users`

Всеки changeSet има уникален ID (базиран на timestamp, напр. `1768335263961-1`) и автор (`Ivaylo.Iliev`), което осигурява **пълна проследимост** на всяка промяна.

Liquibase се стартира **автоматично** при зареждане на Spring Boot приложението (`spring.liquibase.enabled=true`), като проверява кои changeSets вече са приложени (чрез таблица `DATABASECHANGELOG` в PostgreSQL) и изпълнява само новите.

Значението на Liquibase е **критично** за проекта: тя гарантира, че базата данни е винаги в консистентно състояние, независимо дали се стартира на локална машина или в Docker контейнер. Rollback стратегиите позволяват безопасно оттегляне на проблемни миграции, а декларативният формат (YAML/XML) прави промените четими и лесни за review в Git.

---

## 15. TypeScript

### 15.1. Какво е TypeScript?

TypeScript е **статично типизирано надмножество на JavaScript**, разработено от Microsoft. Той добавя типова система, интерфейси, enums, generics, decorators и модерни ECMAScript функции към JavaScript. TypeScript компилира се до стандартен JavaScript и може да работи във всяка среда, поддържаща JavaScript.

Главното предимство на TypeScript е **ранното откриване на грешки** — компилаторът открива type mismatches, липсващи свойства, неправилни аргументи и други проблеми преди runtime, значително намалявайки броя на бъговете.

### 15.2. Роля в YTShare

TypeScript е езикът на целия **Angular frontend** на YTShare. Проектът използва **TypeScript 5.8**, което е най-новата stable версия. Ето как TypeScript се прилага конкретно:

- **`AuthService`** — демонстрира явни TypeScript типове: `signal<User | null>`, `computed<AuthUser | null>`, `Observable<UserCredential>`. Интерфейсите `AuthUser` и `RegisterData` дефинират ясни контракти за данните.
- **`authInterceptor`** — типизиран като `HttpInterceptorFn`, което гарантира правилната сигнатура на функцията.
- **`authGuard` и `guestGuard`** — типизирани като `CanActivateFn`, осигуряващи типова безопасност за route guards.
- **`environment.ts`** — конфигурационните обекти са типизирани, предотвратявайки грешно изписване на Firebase настройки.
- **`app.routes.ts`** — маршрутите са типизирани чрез `Routes`, което гарантира, че lazy-loaded компонентите отговарят на очаквания интерфейс.

TypeScript е **задължителен** за Angular проекти — Angular framework-ът е написан на TypeScript и очаква application код също да бъде на TypeScript. Но дори отвъд изискването на Angular, TypeScript значително подобрява Developer Experience чрез **автоматично допълване**, inline документация и рефакторинг поддръжка в IDE.

---

## 16. Angular

### 16.1. Какво е Angular?

Angular е **пълноценен frontend framework** за създаване на Single Page Applications (SPA), разработен от Google. За разлика от библиотеки като React, Angular е **framework** — той предоставя „от кутията" решения за маршрутизация, формуляри, HTTP комуникация, dependency injection, state management, тестване и интернационализация.

Angular използва компонентна архитектура, където всеки компонент инкапсулира своя шаблон (HTML), стилове (CSS/SCSS) и логика (TypeScript).

### 16.2. Роля в YTShare

Angular е framework-ът на **уеб frontend-а (secondary frontend)** на YTShare. Проектът използва **Angular 20** — най-новата версия, с модерни функции. Фронтендът е организиран по следната структура:

**Архитектурни решения:**
- **Standalone Components** — проектът използва standalone components (без NgModules), което е модерният подход в Angular. Компонентите се зареждат lazy чрез `loadComponent: () => import(...)`.
- **Angular Signals** — `AuthService` използва Angular Signals (`signal`, `computed`) за реактивно управление на състоянието на автентикация, вместо RxJS Subjects.
- **Functional Guards и Interceptors** — вместо class-based guards, проектът използва функционални `CanActivateFn` и `HttpInterceptorFn`, което е по-лекият и препоръчван подход.

**Модули и компоненти:**
- **`app.config.ts`** — централна конфигурация на приложението: Zone.js change detection, Router с component input binding, HttpClient с auth interceptor.
- **`app.routes.ts`** — дефинира три маршрута: Home (защитен с `authGuard`), Login и Register (защитени с `guestGuard` за да не могат автентицирани потребители да ги достъпят).
- **`core/services/auth.service.ts`** — Firebase Authentication интеграция с пълен lifecycle: register, login, logout, token management.
- **`core/services/user.service.ts`** — HTTP комуникация с backend API за потребителски данни.
- **`core/interceptors/auth.interceptor.ts`** — автоматично прикачване на Firebase Bearer токен към всяка заявка към backend API URL-а.
- **`core/guards/auth.guard.ts`** — защита на маршрути въз основа на автентикационния статус.
- **`features/auth/login`** и **`features/auth/register`** — компоненти за вход и регистрация.
- **`features/home`** — основният екран за автентицирани потребители.

Angular е избран за уеб интерфейса заради **пълнотата му като framework** — не са необходими десетки отделни библиотеки за маршрутизация, HTTP, forms и т.н. Опционалният подход с Angular дава подредена и предвидима архитектура, идеална за растящ проект.

---

## 17. AWS (Amazon Web Services)

### 17.1. Какво е AWS?

Amazon Web Services (AWS) е **най-голямата облачна платформа в света**, предоставяща над 200 услуги за изчисления, съхранение, бази данни, машинно обучение, мониторинг и много други. AWS позволява на разработчиците да деплоят и мащабират приложения без нужда от собствена физическа инфраструктура.

Основни AWS услуги, релевантни за уеб приложения, включват EC2 (виртуални машини), ECS/EKS (контейнеризация), RDS (managed бази данни), S3 (файлово съхранение), CloudFront (CDN), Route 53 (DNS), и Elastic Beanstalk (PaaS).

### 17.2. Роля в YTShare

AWS е предвидена като **production инфраструктура** за YTShare backend и frontend. Докато основната функционалност на YTShare (споделяне на видеа в LAN) работи изцяло локално, социалните функции (потребителски профили, приятелства, чат, споделени видеа) изискват **централизиран сървър**, достъпен от Интернет.

Проектът е **подготвен за AWS deployment** чрез:

- **Dockerизация** — backend приложението има `Dockerfile`, използващ multi-stage build:
  1. Build stage: `eclipse-temurin:21-jdk-alpine` за компилация с Maven
  2. Runtime stage: `eclipse-temurin:21-jre-alpine` — минимален image само с JRE
  Резултатният Docker image е оптимизиран за размер и сигурност.

- **Docker Compose** — `docker-compose.yml` дефинира цялата инфраструктура:
  - PostgreSQL 16 Alpine контейнер с health checks
  - Backend Spring Boot контейнер с environment variables
  - Мрежа `ytshare-network` за комуникация между контейнерите
  - Persistent volume `postgres_data` за данните на базата

- **Environment-based конфигурация** — всички чувствителни настройки (DB credentials, Firebase credentials) се подават чрез environment variables, което директно съответства на AWS ECS Task Definitions, AWS Secrets Manager, или AWS Parameter Store.

- **Production-ready settings** — `spring.jpa.hibernate.ddl-auto=validate` и batch оптимизации гарантират, че приложението е готово за production натоварване.

AWS ще осигури на YTShare **висока достъпност, автоматично мащабиране и глобален обхват** за социалните функции на приложението. Containerization стратегията позволява лесен deployment чрез AWS ECS (Elastic Container Service) или AWS EKS (Elastic Kubernetes Service).

---

## 18. Заключение

YTShare е проект, който демонстрира **професионален подход към full-stack разработка**, комбинирайки 15 внимателно подбрани технологии в единна, кохерентна архитектура. Всяка технология заема своето специфично място в стека:

| Компонент | Технологии | Предназначение |
|-----------|-----------|----------------|
| **Android App** | Kotlin, Gradle, Jetpack Compose, Material 3 | Мобилен клиент за споделяне на видеа |
| **Desktop Host** | C# | Windows услуга за приемане на линкове |
| **Backend API** | Java, Maven, Spring Boot, Lombok, MapStruct | Централен REST API сървър |
| **Database** | PostgreSQL, Liquibase | Съхранение и версиониране на данни |
| **Authentication** | Firebase Authentication | Единна автентикация за всички клиенти |
| **Web Frontend** | TypeScript, Angular | Уеб интерфейс за социални функции |
| **Infrastructure** | AWS, Docker | Production deployment и мащабиране |

Технологичният стек е избран с мисъл за **модерност** (Kotlin, Compose, Angular 20, Spring Boot 4), **надеждност** (PostgreSQL, Firebase, AWS), **продуктивност** (Lombok, MapStruct, Gradle) и **мащабируемост** (Docker, stateless архитектура, cloud-ready конфигурация).

Проектът успешно решава конкретен потребителски проблем — безпроблемното споделяне на YouTube видеа между устройства — и надгражда над него социална платформа, която добавя стойност чрез общност и персонализация.

---

*Документ създаден: Март 2026 г.*  
*Версия: 1.0*
