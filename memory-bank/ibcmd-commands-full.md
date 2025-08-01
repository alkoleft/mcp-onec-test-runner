# ibcmd — Полное структурированное описание командного интерфейса (1С:Предприятие 8.3.24.1761)

---

## Общие параметры для всех режимов

- `--version`, `-v` — версия утилиты
- `--help`, `-h`, `-?` — справка
- `--pid=<pid>`, `-p <pid>` — идентификатор процесса сервера
- `--remote=<url>`, `-r <url>` — сетевой адрес сервера

---

## Режим: infobase — Управление информационной базой

### Общие параметры режима infobase
- `--config=<path>`, `-c <path>` — путь к конфигурационному файлу
- `--system=<path>` — путь к системному конфигурационному файлу
- `--dbms=<kind>` — тип СУБД (MSSQLServer, PostgreSQL, IBMDB2, OracleDatabase)
- `--database-server=<server>`, `--db-server=<server>` — имя сервера СУБД
- `--database-name=<name>`, `--db-name=<name>` — имя базы данных
- `--database-user=<name>`, `--db-user=<name>` — имя пользователя СУБД
- `--database-password=<password>`, `--db-pwd=<password>` — пароль пользователя СУБД
- `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля через stdin
- `--database-path=<path>`, `--db-path=<path>` — путь к каталогу файловой базы
- `--data=<path>`, `-d <path>` — путь к каталогу данных сервера
- `--lock=<path>` — путь к файлу блокировки
- `--temp=<path>`, `-t <path>` — путь к каталогу временных файлов
- `--users-data=<path>` — путь к каталогу данных пользователей
- `--session-data=<path>` — путь к каталогу сеансовых данных
- `--stt-data=<path>` — путь к каталогу моделей распознавания речи
- `--log-data=<path>` — путь к каталогу журнала регистрации
- `--ftext-data=<path>` — путь к каталогу полнотекстового поиска
- `--ftext2-data=<path>` — путь к каталогу полнотекстового поиска v2
- `--openid-data=<path>` — путь к каталогу OpenID-аутентификации
- `--bin-data-strg=<path>` — путь к каталогу двоичных данных

---

### Команды режима infobase

#### 1. create — Создание информационной базы
- **Описание:** Создаёт новую информационную базу.
- **Параметры:**
  - `--locale=<name>`, `-l <name>` — локаль ИБ
  - `--date-offset=<years>` — смещение дат (для MSSQLServer)
  - `--create-database` — создать БД, если отсутствует
  - `--restore=<file>` — путь к файлу выгрузки для загрузки
  - `--load=<file>` — путь к файлу конфигурации для загрузки
  - `--import=<directory>` — путь к каталогу XML для загрузки
  - `--apply` — выполнить обновление конфигурации после загрузки
  - `--force`, `-F` — подтверждение при наличии предупреждений
- **Пример:**
  ```sh
  ibcmd infobase create --dbms=PostgreSQL --database-server=localhost --database-name=demo --user=Админ --password=123
  ```

#### 2. dump — Выгрузка данных информационной базы
- **Описание:** Выгружает данные ИБ в файл.
- **Параметры:**
  - `--user=<name>`, `-u <name>` — пользователь ИБ
  - `--password=<password>`, `-P <password>` — пароль пользователя
  - `<path>` — путь к файлу выгрузки
- **Пример:**
  ```sh
  ibcmd infobase dump --user=Админ --password=123 /tmp/ib1.dt
  ```

#### 3. restore — Загрузка данных информационной базы
- **Описание:** Загружает данные из файла выгрузки в ИБ.
- **Параметры:**
  - `--user=<name>`, `-u <name>` — пользователь ИБ
  - `--password=<password>`, `-P <password>` — пароль пользователя
  - `--create-database` — создать БД, если отсутствует
  - `--force`, `-F` — принудительное завершение сеансов
  - `<path>` — путь к файлу выгрузки
- **Пример:**
  ```sh
  ibcmd infobase restore --user=Админ --password=123 /tmp/ib1.dt
  ```

#### 4. clear — Очистка информационной базы
- **Описание:** Очищает информационную базу.
- **Параметры:**
  - `--user=<name>`, `-u <name>` — пользователь ИБ
  - `--password=<password>`, `-P <password>` — пароль пользователя
- **Пример:**
  ```sh
  ibcmd infobase clear --user=Админ --password=123
  ```

#### 5. replicate — Репликация информационной базы
- **Описание:** Копирует данные между ИБ или СУБД.
- **Параметры:**
  - `--target-dbms=<kind>` — тип целевой СУБД
  - `--target-database-server=<server>`, `--target-db-server=<server>` — сервер целевой СУБД
  - `--target-database-name=<name>`, `--target-db-name=<name>` — имя целевой БД
  - `--target-database-user=<name>`, `--target-db-user=<name>` — пользователь целевой СУБД
  - `--target-database-password=<password>`, `--target-db-pwd=<password>` — пароль целевой СУБД
  - `--target-request-database-password`, `--target-request-db-pwd` — запрос пароля целевой СУБД
  - `--target-database-path=<path>`, `--target-db-path=<path>` — путь к целевой файловой БД
  - `--target-create-database` — создать целевую БД, если отсутствует
  - `--target-date-offset=<years>` — смещение дат в целевой БД
  - `--force` — принудительное завершение сеансов
  - `--jobs-count=<n>`, `-j <n>` — количество потоков выгрузки
  - `--target-jobs-count=<n>`, `-J <n>` — количество потоков загрузки
  - `--batch-size=<n>`, `-B <n>` — размер пакета строк
  - `--batch-data-size=<n>` — размер пакета данных (байт)
- **Пример:**
  ```sh
  ibcmd infobase replicate --target-dbms=PostgreSQL --target-database-server=localhost --target-database-name=targetdb
  ```

#### 6. config — Управление конфигурацией информационной базы
- **Описание:** Позволяет загружать, выгружать, проверять, применять, сбрасывать, восстанавливать, экспортировать, импортировать конфигурацию, а также управлять поддержкой, разделителями, расширениями, идентификатором поколения и подписью.
- **См. отдельный раздел ниже**

#### 7. extension — Управление расширениями
- **Описание:** Создание, получение информации, список, обновление, удаление расширений.
- **Параметры:**
  - `--name=<name>` — имя расширения
  - `--name-prefix=<prefix>` — префикс имен
  - `--synonym=<synonym>` — синоним
  - `--purpose=<customization|add-on|patch>` — назначение
  - `--active=<yes|no>` — активность
  - `--safe-mode=<yes|no>` — безопасный режим
  - `--security-profile-name=<yes|no>` — профиль безопасности
  - `--unsafe-action-protection=<yes|no>` — защита от опасных действий
  - `--used-in-distributed-infobase=<yes|no>` — используется в распределённой ИБ
  - `--scope=<infobase|data-separation>` — область действия
  - `--all` — удалить все расширения
- **Пример:**
  ```sh
  ibcmd infobase extension create --name=MyExt --name-prefix=ME --purpose=add-on
  ```

#### 8. generation-id — Получить идентификатор поколения конфигурации
- **Описание:** Получает идентификатор поколения конфигурации.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
- **Пример:**
  ```sh
  ibcmd infobase generation-id --extension=MyExt
  ```

#### 9. sign — Цифровая подпись конфигурации/расширения
- **Описание:** Подписывает конфигурацию или расширение.
- **Параметры:**
  - `--key=<path>`, `-k <path>` — путь к приватному ключу
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--db` — операция над конфигурацией базы данных
  - `--out=<path>`, `-o <path>` — путь для подписанной копии
  - `<path>` — путь к файлу для подписи
- **Пример:**
  ```sh
  ibcmd infobase sign --key=/keys/key.pem --out=/tmp/signed.cf /tmp/config.cf
  ```

---

## Режим: server — Настройка автономного сервера

### Команды режима server

#### 1. config init — Инициализация конфигурации автономного сервера
- **Описание:** Создаёт конфигурационный файл для автономного сервера.
- **Параметры:**
  - `--out=<file>`, `-o <file>` — путь к файлу для записи конфигурации
  - `--http-address=<address>`, `--address=<address>`, `-a <address>` — IP адрес сервера (localhost, any, IPv4, IPv6)
  - `--http-port=<number>`, `--port=<number>`, `-p <number>` — TCP порт (по умолчанию: 8314)
  - `--http-base=<location>`, `--base=<location>`, `-b <location>` — базовый путь публикации (по умолчанию: /)
  - `--name=<name>`, `-n <name>` — имя информационной базы
  - `--id=<uuid>` — идентификатор ИБ (UUID или auto)
  - `--dbms=<kind>` — тип СУБД
  - `--database-server=<server>`, `--db-server=<server>` — сервер СУБД
  - `--database-name=<name>`, `--db-name=<name>` — имя базы данных
  - `--database-user=<name>`, `--db-user=<name>` — пользователь СУБД
  - `--database-password=<password>`, `--db-pwd=<password>` — пароль СУБД
  - `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля СУБД
  - `--database-path=<path>`, `--db-path=<path>` — путь к файловой БД
  - `--distribute-licenses=<flag>` — выдача клиентских лицензий (allow/deny)
  - `--schedule-jobs=<flag>` — планирование регламентных заданий (allow/deny)
  - `--disable-local-speech-to-text=<flag>` — запрет локального распознавания речи (yes/no)
- **Пример:**
  ```sh
  ibcmd server config init --out=/etc/1c/server.conf --http-address=any --http-port=8314 --name=DemoIB
  ```

#### 2. config import — Импорт конфигурации из кластера серверов 1С
- **Описание:** Импортирует конфигурацию из кластера серверов 1С:Предприятие.
- **Параметры:**
  - `--cluster-data=<path>` — путь к каталогу данных центрального сервера
  - `--manager-port=<port>` — порт менеджера кластера (по умолчанию: 1541)
  - `--name=<name>`, `-n <name>` — имя информационной базы (обязательно)
  - `--out=<file>`, `-o <file>` — путь к файлу для записи конфигурации
  - `--address=<address>`, `-a <address>` — IP адрес сервера
  - `--port=<number>` — TCP порт
  - `--base=<location>`, `-b <location>` — базовый путь публикации
  - `--publication=<path>`, `-p <path>` — путь к файлу дескриптора публикации
- **Пример:**
  ```sh
  ibcmd server config import --cluster-data=/opt/1c/cluster --name=DemoIB --out=/etc/1c/server.conf
  ```

---

## Режим: config — Работа с конфигурациями и расширениями

### Общие параметры режима config
- `--config=<path>`, `-c <path>` — путь к конфигурационному файлу
- `--system=<path>` — путь к системному конфигурационному файлу
- `--dbms=<kind>` — тип СУБД
- `--database-server=<server>`, `--db-server=<server>` — сервер СУБД
- `--database-name=<name>`, `--db-name=<name>` — имя базы данных
- `--database-user=<name>`, `--db-user=<name>` — пользователь СУБД
- `--database-password=<password>`, `--db-pwd=<password>` — пароль СУБД
- `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля СУБД
- `--database-path=<path>`, `--db-path=<path>` — путь к файловой БД
- `--data=<path>`, `-d <path>` — путь к каталогу данных сервера
- `--lock=<path>` — путь к файлу блокировки
- `--temp=<path>`, `-t <path>` — путь к каталогу временных файлов
- `--users-data=<path>` — путь к каталогу данных пользователей
- `--session-data=<path>` — путь к каталогу сеансовых данных
- `--stt-data=<path>` — путь к каталогу моделей распознавания речи
- `--log-data=<path>` — путь к каталогу журнала регистрации
- `--ftext-data=<path>` — путь к каталогу полнотекстового поиска
- `--ftext2-data=<path>` — путь к каталогу полнотекстового поиска v2
- `--openid-data=<path>` — путь к каталогу OpenID-аутентификации
- `--bin-data-strg=<path>` — путь к каталогу двоичных данных
- `--user=<name>`, `-u <name>` — пользователь информационной базы
- `--password=<password>`, `-P <password>` — пароль пользователя

### Команды режима config

#### 1. load — Загрузка конфигурации
- **Описание:** Загружает конфигурацию из файла в информационную базу.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--force`, `-F` — подтверждение при наличии предупреждений
  - `<path>` — путь к файлу конфигурации
- **Пример:**
  ```sh
  ibcmd config load --user=Админ --password=123 /tmp/config.cf
  ```

#### 2. save — Выгрузка конфигурации
- **Описание:** Выгружает конфигурацию из информационной базы в файл.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--db` — операция над конфигурацией базы данных
  - `<path>` — путь к файлу конфигурации
- **Пример:**
  ```sh
  ibcmd config save --user=Админ --password=123 /tmp/config.cf
  ```

#### 3. check — Проверка конфигурации
- **Описание:** Проверяет корректность конфигурации.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--force`, `-F` — подтверждение при наличии предупреждений
- **Пример:**
  ```sh
  ibcmd config check --user=Админ --password=123
  ```

#### 4. apply — Обновление конфигурации базы данных
- **Описание:** Применяет конфигурацию к базе данных.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--force`, `-F` — подтверждение при наличии предупреждений
  - `--dynamic=<auto|disable|prompt|force>` — динамическое обновление
  - `--session-terminate=<disable|prompt|force>` — завершение сеансов
- **Пример:**
  ```sh
  ibcmd config apply --user=Админ --password=123 --dynamic=auto
  ```

#### 5. reset — Возврат к конфигурации базы данных
- **Описание:** Возвращает конфигурацию к состоянию базы данных.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
- **Пример:**
  ```sh
  ibcmd config reset --user=Админ --password=123
  ```

#### 6. repair — Восстановление конфигурации после незавершённой операции
- **Описание:** Восстанавливает конфигурацию после сбоя операции.
- **Параметры:**
  - `--commit` — завершить незавершённую операцию
  - `--rollback` — отменить незавершённую операцию
  - `--fix-metadata` — восстановить структуру метаданных
- **Пример:**
  ```sh
  ibcmd config repair --commit --user=Админ --password=123
  ```

#### 7. export — Экспорт конфигурации в XML
- **Описание:** Экспортирует конфигурацию в XML формат.
- **Подкоманды:**
  - `info` — информация о состоянии конфигурации
  - `status` — информация об изменениях конфигурации
  - `objects` — экспорт выбранных объектов
  - `all-extensions` — экспорт всех расширений
- **Параметры:**
  - `--base=<file>`, `-b <file>` — файл информации о конфигурации
  - `--file=<file>`, `-f <file>` — файл конфигурации
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--sync` — синхронизация с конфигурацией
  - `--force` — полная выгрузка
  - `--threads=<n>`, `-T <n>` — количество потоков
  - `--archive`, `-A` — упаковать в архив
  - `--ignore-unresolved-refs` — игнорировать неразрешимые ссылки
  - `<path>` — путь к каталогу экспорта
- **Пример:**
  ```sh
  ibcmd config export --user=Админ --password=123 /tmp/export
  ```

#### 8. import — Импорт конфигурации из XML
- **Описание:** Импортирует конфигурацию из XML формата.
- **Подкоманды:**
  - `files` — импорт выбранных файлов
  - `all-extensions` — импорт всех расширений
- **Параметры:**
  - `--out=<file>`, `-o <file>` — файл для записи конфигурации
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--base-dir=<directory>` — базовый каталог XML файлов
  - `--archive=<path>` — путь к архиву XML файлов
  - `--no-check` — отключить проверку метаданных
  - `--partial` — разрешить частичный набор файлов
  - `<path>` — путь к каталогу или архиву XML
- **Пример:**
  ```sh
  ibcmd config import --user=Админ --password=123 /tmp/import
  ```

#### 9. support disable — Снятие конфигурации с поддержки
- **Описание:** Снимает конфигурацию с поддержки.
- **Параметры:**
  - `--force`, `-F` — снятие с поддержки принудительно
- **Пример:**
  ```sh
  ibcmd config support disable --user=Админ --password=123 --force
  ```

#### 10. data-separation list — Список разделителей информационной базы
- **Описание:** Выводит список разделителей информационной базы.
- **Пример:**
  ```sh
  ibcmd config data-separation list --user=Админ --password=123
  ```

#### 11. extension — Управление расширениями конфигурации
- **Описание:** Создание, получение информации, список, обновление, удаление расширений.
- **Подкоманды:**
  - `create` — создание расширения
  - `info` — информация о расширении
  - `list` — список расширений
  - `update` — обновление свойств расширения
  - `delete` — удаление расширения
- **Параметры:**
  - `--name=<name>` — имя расширения
  - `--name-prefix=<prefix>` — префикс имен
  - `--synonym=<synonym>` — синоним
  - `--purpose=<customization|add-on|patch>` — назначение
  - `--active=<yes|no>` — активность
  - `--safe-mode=<yes|no>` — безопасный режим
  - `--security-profile-name=<yes|no>` — профиль безопасности
  - `--unsafe-action-protection=<yes|no>` — защита от опасных действий
  - `--used-in-distributed-infobase=<yes|no>` — используется в распределённой ИБ
  - `--scope=<infobase|data-separation>` — область действия
  - `--all` — удалить все расширения
- **Пример:**
  ```sh
  ibcmd config extension create --name=MyExt --name-prefix=ME --purpose=add-on
  ```

#### 12. generation-id — Идентификатор поколения конфигурации
- **Описание:** Получает идентификатор поколения конфигурации.
- **Параметры:**
  - `--extension=<extension>`, `-e <extension>` — имя расширения
- **Пример:**
  ```sh
  ibcmd config generation-id --user=Админ --password=123
  ```

#### 13. sign — Цифровая подпись конфигурации/расширения
- **Описание:** Подписывает конфигурацию или расширение.
- **Параметры:**
  - `--key=<path>`, `-k <path>` — путь к приватному ключу
  - `--extension=<extension>`, `-e <extension>` — имя расширения
  - `--db` — операция над конфигурацией базы данных
  - `--out=<path>`, `-o <path>` — путь для подписанной копии
  - `<path>` — путь к файлу для подписи
- **Пример:**
  ```sh
  ibcmd config sign --key=/keys/key.pem --out=/tmp/signed.cf /tmp/config.cf
  ```

---

## Режим: session — Администрирование сеансов

### Команды режима session

#### 1. info — Получение информации о сеансе
- **Описание:** Получает подробную информацию о конкретном сеансе.
- **Параметры:**
  - `--session=<uuid>` — идентификатор сеанса (обязательно)
  - `--licenses` — вывод информации о лицензиях
- **Пример:**
  ```sh
  ibcmd session info --session=12345678-1234-1234-1234-123456789012
  ```

#### 2. list — Получение списка сеансов
- **Описание:** Получает список всех активных сеансов.
- **Параметры:**
  - `--licenses` — вывод информации о лицензиях
- **Пример:**
  ```sh
  ibcmd session list
  ```

#### 3. terminate — Принудительное завершение сеанса
- **Описание:** Принудительно завершает указанный сеанс.
- **Параметры:**
  - `--session=<uuid>` — идентификатор сеанса (обязательно)
  - `--error-message=<string>` — сообщение о причине завершения
- **Пример:**
  ```sh
  ibcmd session terminate --session=12345678-1234-1234-1234-123456789012 --error-message="Плановое завершение"
  ```

#### 4. interrupt-current-server-call — Прерывание текущего серверного вызова
- **Описание:** Прерывает текущий серверный вызов в указанном сеансе.
- **Параметры:**
  - `--session=<uuid>` — идентификатор сеанса (обязательно)
  - `--error-message=<string>` — сообщение о причине прерывания
- **Пример:**
  ```sh
  ibcmd session interrupt-current-server-call --session=12345678-1234-1234-1234-123456789012 --error-message="Прерывание вызова"
  ```

---

## Режим: lock — Администрирование блокировок

### Команды режима lock

#### 1. list — Получение списка блокировок
- **Описание:** Получает список всех активных блокировок в информационной базе.
- **Параметры:**
  - `--session=<uuid>` — идентификатор сеанса (опционально, для фильтрации по сеансу)
- **Пример:**
  ```sh
  ibcmd lock list
  ibcmd lock list --session=12345678-1234-1234-1234-123456789012
  ```

---

## Режим: mobile-app — Работа с мобильным приложением

### Общие параметры режима mobile-app
- `--config=<path>`, `-c <path>` — путь к конфигурационному файлу
- `--system=<path>` — путь к системному конфигурационному файлу
- `--dbms=<kind>` — тип СУБД (MSSQLServer, PostgreSQL, IBMDB2, OracleDatabase)
- `--database-server=<server>`, `--db-server=<server>` — имя сервера СУБД
- `--database-name=<name>`, `--db-name=<name>` — имя базы данных
- `--database-user=<name>`, `--db-user=<name>` — имя пользователя СУБД
- `--database-password=<password>`, `--db-pwd=<password>` — пароль пользователя СУБД
- `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля через stdin
- `--database-path=<path>`, `--db-path=<path>` — путь к каталогу файловой базы
- `--data=<path>`, `-d <path>` — путь к каталогу данных сервера
- `--lock=<path>` — путь к файлу блокировки
- `--temp=<path>`, `-t <path>` — путь к каталогу временных файлов
- `--users-data=<path>` — путь к каталогу данных пользователей
- `--session-data=<path>` — путь к каталогу сеансовых данных
- `--stt-data=<path>` — путь к каталогу моделей распознавания речи
- `--log-data=<path>` — путь к каталогу журнала регистрации
- `--ftext-data=<path>` — путь к каталогу полнотекстового поиска
- `--ftext2-data=<path>` — путь к каталогу полнотекстового поиска v2
- `--openid-data=<path>` — путь к каталогу OpenID-аутентификации
- `--bin-data-strg=<path>` — путь к каталогу двоичных данных
- `--user=<name>`, `-u <name>` — пользователь информационной базы
- `--password=<password>`, `-P <password>` — пароль пользователя

### Команды режима mobile-app

#### 1. export — Экспорт мобильного приложения
- **Описание:** Экспортирует мобильное приложение для развертывания.
- **Параметры:**
  - `<path>` — путь для экспорта мобильного приложения
- **Пример:**
  ```sh
  ibcmd mobile-app export --user=Админ --password=123 /tmp/mobile-app
  ```

---

## Режим: mobile-client — Работа с мобильным клиентом

### Общие параметры режима mobile-client
- `--config=<path>`, `-c <path>` — путь к конфигурационному файлу
- `--system=<path>` — путь к системному конфигурационному файлу
- `--dbms=<kind>` — тип СУБД (MSSQLServer, PostgreSQL, IBMDB2, OracleDatabase)
- `--database-server=<server>`, `--db-server=<server>` — имя сервера СУБД
- `--database-name=<name>`, `--db-name=<name>` — имя базы данных
- `--database-user=<name>`, `--db-user=<name>` — имя пользователя СУБД
- `--database-password=<password>`, `--db-pwd=<password>` — пароль пользователя СУБД
- `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля через stdin
- `--database-path=<path>`, `--db-path=<path>` — путь к каталогу файловой базы
- `--data=<path>`, `-d <path>` — путь к каталогу данных сервера
- `--lock=<path>` — путь к файлу блокировки
- `--temp=<path>`, `-t <path>` — путь к каталогу временных файлов
- `--users-data=<path>` — путь к каталогу данных пользователей
- `--session-data=<path>` — путь к каталогу сеансовых данных
- `--stt-data=<path>` — путь к каталогу моделей распознавания речи
- `--log-data=<path>` — путь к каталогу журнала регистрации
- `--ftext-data=<path>` — путь к каталогу полнотекстового поиска
- `--ftext2-data=<path>` — путь к каталогу полнотекстового поиска v2
- `--openid-data=<path>` — путь к каталогу OpenID-аутентификации
- `--bin-data-strg=<path>` — путь к каталогу двоичных данных
- `--user=<name>`, `-u <name>` — пользователь информационной базы
- `--password=<password>`, `-P <password>` — пароль пользователя

### Команды режима mobile-client

#### 1. export — Экспорт мобильного клиента
- **Описание:** Экспортирует мобильный клиент для развертывания.
- **Параметры:**
  - `<path>` — путь для экспорта мобильного клиента
- **Пример:**
  ```sh
  ibcmd mobile-client export --user=Админ --password=123 /tmp/mobile-client
  ```

#### 2. sign — Цифровая подпись мобильного клиента
- **Описание:** Подписывает мобильный клиент цифровой подписью.
- **Параметры:**
  - `--key=<path>`, `-k <path>` — путь к приватному ключу (обязательно, формат .pem)
- **Пример:**
  ```sh
  ibcmd mobile-client sign --key=/keys/key.pem --user=Админ --password=123
  ```

---

## Режим: extension — Работа с расширениями

### Общие параметры режима extension
- `--config=<path>`, `-c <path>` — путь к конфигурационному файлу
- `--system=<path>` — путь к системному конфигурационному файлу
- `--dbms=<kind>` — тип СУБД (MSSQLServer, PostgreSQL, IBMDB2, OracleDatabase)
- `--database-server=<server>`, `--db-server=<server>` — имя сервера СУБД
- `--database-name=<name>`, `--db-name=<name>` — имя базы данных
- `--database-user=<name>`, `--db-user=<name>` — имя пользователя СУБД
- `--database-password=<password>`, `--db-pwd=<password>` — пароль пользователя СУБД
- `--request-database-password`, `--request-db-pwd`, `-W` — запрос пароля через stdin
- `--database-path=<path>`, `--db-path=<path>` — путь к каталогу файловой базы
- `--data=<path>`, `-d <path>` — путь к каталогу данных сервера
- `--lock=<path>` — путь к файлу блокировки
- `--temp=<path>`, `-t <path>` — путь к каталогу временных файлов
- `--users-data=<path>` — путь к каталогу данных пользователей
- `--session-data=<path>` — путь к каталогу сеансовых данных
- `--stt-data=<path>` — путь к каталогу моделей распознавания речи
- `--log-data=<path>` — путь к каталогу журнала регистрации
- `--ftext-data=<path>` — путь к каталогу полнотекстового поиска
- `--ftext2-data=<path>` — путь к каталогу полнотекстового поиска v2
- `--openid-data=<path>` — путь к каталогу OpenID-аутентификации
- `--bin-data-strg=<path>` — путь к каталогу двоичных данных

### Команды режима extension

#### 1. create — Создание расширения
- **Описание:** Создаёт новое расширение конфигурации.
- **Параметры:**
  - `--name=<name>` — имя расширения (обязательно, должно начинаться с буквы, содержать только буквы, цифры и "_")
  - `--name-prefix=<prefix>` — префикс имен (обязательно, правила те же)
  - `--synonym=<synonym>` — синоним в формате функции NStr()
  - `--purpose=<customization|add-on|patch>` — назначение расширения
- **Пример:**
  ```sh
  ibcmd extension create --name=MyExtension --name-prefix=ME --purpose=add-on
  ```

#### 2. info — Получение информации о расширении
- **Описание:** Получает подробную информацию о конкретном расширении.
- **Параметры:**
  - `--name=<name>` — имя расширения (обязательно)
- **Пример:**
  ```sh
  ibcmd extension info --name=MyExtension
  ```

#### 3. list — Получение списка расширений
- **Описание:** Получает список всех расширений в информационной базе.
- **Пример:**
  ```sh
  ibcmd extension list
  ```

#### 4. update — Обновление свойств расширения
- **Описание:** Обновляет свойства указанного расширения.
- **Параметры:**
  - `--name=<name>` — имя расширения (обязательно)
  - `--active=<yes|no>` — активность расширения
  - `--safe-mode=<yes|no>` — безопасный режим
  - `--security-profile-name=<yes|no>` — имя профиля безопасности
  - `--unsafe-action-protection=<yes|no>` — защита от опасных действий
  - `--used-in-distributed-infobase=<yes|no>` — используется в распределённой ИБ
  - `--scope=<infobase|data-separation>` — область действия расширения
- **Пример:**
  ```sh
  ibcmd extension update --name=MyExtension --active=yes --safe-mode=yes
  ```

#### 5. delete — Удаление расширения
- **Описание:** Удаляет указанное расширение или все расширения.
- **Параметры:**
  - `--name=<name>` — имя расширения для удаления
  - `--all` — удалить все расширения
- **Пример:**
  ```sh
  ibcmd extension delete --name=MyExtension
  ibcmd extension delete --all
  ```

---

## Полный список поддерживаемых режимов

1. **infobase** — Управление информационной базой
2. **server** — Настройка автономного сервера
3. **config** — Работа с конфигурациями и расширениями
4. **session** — Администрирование сеансов
5. **lock** — Администрирование блокировок
6. **mobile-app** — Работа с мобильным приложением
7. **mobile-client** — Работа с мобильным клиентом
8. **extension** — Работа с расширениями

---

## Общие примеры использования

### Создание и настройка информационной базы
```sh
# Создание файловой ИБ
ibcmd infobase create --database-path=/data/ib1 --user=Админ --password=123

# Выгрузка данных
ibcmd infobase dump --user=Админ --password=123 /tmp/ib1.dt

# Загрузка данных
ibcmd infobase restore --user=Админ --password=123 /tmp/ib1.dt

# Проверка конфигурации
ibcmd infobase config check --user=Админ --password=123
```

### Управление сеансами
```sh
# Получение списка сеансов
ibcmd session list

# Завершение сеанса
ibcmd session terminate --session=12345678-1234-1234-1234-123456789012
```

### Работа с конфигурациями
```sh
# Загрузка конфигурации
ibcmd config load --user=Админ --password=123 /tmp/config.cf

# Выгрузка конфигурации
ibcmd config save --user=Админ --password=123 /tmp/config.cf

# Применение конфигурации
ibcmd config apply --user=Админ --password=123 --dynamic=auto
```

### Управление расширениями
```sh
# Создание расширения
ibcmd extension create --name=MyExt --name-prefix=ME --purpose=add-on

# Список расширений
ibcmd extension list

# Обновление свойств
ibcmd extension update --name=MyExt --active=yes
```

---

## Справка по конкретным командам

Для получения подробной справки по конкретной команде используйте:
```sh
ibcmd help <режим>
ibcmd <режим> --help
```

Например:
```sh
ibcmd help infobase
ibcmd infobase create --help
```