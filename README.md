# Orders CRUD — Servlet + Apache Tomcat

Домашнє завдання: сервлет, що виконує CRUD-операції над сутністю `Order`
(яка містить список `Product`), розгорнутий на Apache Tomcat. JSON
(де)серіалізація — через Jackson. Юніт-тести — JUnit 5 + Mockito.

## Структура проєкту

```
homework-tomcat-crud/
├── pom.xml
├── src/main/java/com/example/orders/
│   ├── model/
│   │   ├── Order.java          # id, date, cost, products
│   │   └── Product.java        # id, name, cost
│   ├── repository/
│   │   ├── OrderRepository.java          # інтерфейс (для мокування в тестах)
│   │   └── InMemoryOrderRepository.java  # реалізація "в пам'яті"
│   ├── servlet/
│   │   └── OrderServlet.java   # CRUD: POST / GET / PUT / DELETE
│   ├── exception/
│   │   └── OrderNotFoundException.java
│   └── util/
│       └── JsonUtil.java       # спільний налаштований ObjectMapper
├── src/main/webapp/WEB-INF/web.xml
└── src/test/java/com/example/orders/servlet/
    └── OrderServletTest.java   # JUnit 5 + Mockito
```

## 1. Встановлення Apache Tomcat

1. Встановіть JDK 11+ (`java -version`).
2. Завантажте Apache Tomcat 9.x (наприклад, `apache-tomcat-9.0.xx`) з
   https://tomcat.apache.org/download-90.cgi
3. Розпакуйте архів, наприклад у `/opt/tomcat` (Linux/macOS) або
   `C:\tomcat` (Windows).
4. (Опційно) відкрийте доступ до менеджера додатків, додавши користувача
   у `conf/tomcat-users.xml`:

   ```xml
   <role rolename="manager-gui"/>
   <role rolename="manager-script"/>
   <user username="admin" password="admin" roles="manager-gui,manager-script"/>
   ```

5. Запустіть сервер:
   - Linux/macOS: `sh $CATALINA_HOME/bin/startup.sh`
   - Windows: `%CATALINA_HOME%\bin\startup.bat`

   Перевірте, що Tomcat піднявся: http://localhost:8080

> **Примітка щодо версій.** Проєкт написаний під Tomcat 9 (Servlet API 4.0,
> пакет `javax.servlet.*`). Якщо ви використовуєте Tomcat 10+, потрібно:
> замінити залежність у `pom.xml` на `jakarta.servlet:jakarta.servlet-api:5.0.0`
> та замінити всі імпорти `javax.servlet.*` на `jakarta.servlet.*` у
> `OrderServlet.java`.

## 2. Збірка проєкту

```bash
cd homework-tomcat-crud
mvn clean package
```

Це створить `target/orders-crud.war`.

## 3. Запуск юніт-тестів

```bash
mvn test
```

Тести знаходяться в `OrderServletTest` і перевіряють усі чотири CRUD-методи
сервлету (`doPost`, `doGet`, `doPut`, `doDelete`), включно з граничними
випадками (відсутній `id`, некоректний JSON, неіснуюче замовлення тощо).
Репозиторій замовлень мокається через Mockito, тож тести не залежать від
реального Tomcat чи мережі.

## 4. Деплой на Tomcat

Скопіюйте зібраний `.war` у теку `webapps` вашого Tomcat:

```bash
cp target/orders-crud.war $CATALINA_HOME/webapps/
```

Tomcat автоматично розпакує та розгорне застосунок за адресою:

```
http://localhost:8080/orders-crud/orders
```

## 5. Приклади запитів (curl)

**Створити замовлення (POST):**

```bash
curl -X POST http://localhost:8080/orders-crud/orders \
  -H "Content-Type: application/json" \
  -d '{
        "date": "2026-09-03",
        "products": [
          { "name": "Клавіатура", "cost": 799.99 },
          { "name": "Мишка", "cost": 349.50 }
        ]
      }'
```

Відповідь `201 Created` з JSON, що містить згенерований `id` замовлення,
`id` кожного продукту та автоматично порахований `cost` (сума продуктів).

**Отримати замовлення по id (GET):**

```bash
curl "http://localhost:8080/orders-crud/orders?id=1"
```

**Оновити замовлення (PUT):**

```bash
curl -X PUT "http://localhost:8080/orders-crud/orders?id=1" \
  -H "Content-Type: application/json" \
  -d '{
        "date": "2026-09-04",
        "products": [
          { "name": "Клавіатура", "cost": 899.00 }
        ]
      }'
```

**Видалити замовлення (DELETE):**

```bash
curl -X DELETE "http://localhost:8080/orders-crud/orders?id=1"
```

Повертає `204 No Content`, якщо видалено, або `404 Not Found`, якщо
замовлення з таким `id` не існує.

## 6. Здача домашнього завдання через git

Відповідно до умови завдання:

```bash
# якщо створюєте новий репозиторій
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin <URL_вашого_репозиторію>
git push -u origin main

# створення робочої гілки для домашки
git checkout -b feature/orders-crud-servlet
git add .
git commit -m "Add Order/Product CRUD servlet with Jackson, JUnit5 + Mockito tests"
git push -u origin feature/orders-crud-servlet
```

Потім на GitHub створіть Pull Request із гілки `feature/orders-crud-servlet`
у `main` та надішліть посилання на PR на перевірку.

## Про реалізацію

- **Зберігання даних**: у завданні не вказана конкретна БД, тому для
  простоти й самодостатності прикладу використано `ConcurrentHashMap`
  (`InMemoryOrderRepository`). Логіка збереження винесена в інтерфейс
  `OrderRepository`, тож за потреби легко підмінити реалізацію на, скажімо,
  JDBC/Hibernate-репозиторій — сервлет від цього не зміниться.
- **Тестованість**: `OrderServlet` має конструктор, що приймає
  `OrderRepository`, — це і дозволяє в тестах підставляти Mockito-мок
  замість реального сховища.
- **cost замовлення** рахується автоматично як сума `cost` усіх продуктів
  при створенні/оновленні (можна прибрати цю логіку й дозволити передавати
  `cost` вручну, якщо це не відповідає вимогам викладача).
