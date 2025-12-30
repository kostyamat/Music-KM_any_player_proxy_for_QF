<p align="center">
  <a href="https://www.paypal.com/paypalme/kostyamat">
    <img src="https://thumbs.dreamstime.com/b/cute-kawaii-coffee-mug-character-smiling-steam-isolated-white-adorable-cartoon-happy-face-decorative-lace-pattern-401912575.jpg" alt="Buy me a coffee" width="200"/>
    <br>
    <strong>If you found my work helpful, buy me a coffee! It keeps me motivated ☕</strong>
  </a>
</p>



# Music-KM

[Українська](#українська) | [English](#english)

---

## <a name="english"></a>English Description

**Music-KM** is a utility application for QF001 / Roco K706 (UIS 7862A/7862S) Android head units. It allows you to replace the standard music player in the system's media carousel with any third-party player of your choice (e.g., Poweramp, Pulsar, etc.).

### How it Works

This application integrates into the system's "Mode" button cycle:

1.  It registers itself as the system "Music" app.
2.  When you select "Music" via the "Mode" button, Music-KM intercepts the command.
3.  It then immediately launches the actual music player you have selected in its settings.
4.  It cleverly holds the system's focus by covering the player with a transparent window, preventing the media carousel from switching to the next source.

This creates a seamless experience, making your favorite player feel like a native part of the car's system.

### Installation Requirements (Crucial!)

To install this app as a system update and successfully bypass signature verification, you MUST meet the following requirements:

*   **Root Access** is required.
*   **Magisk** must be installed.
*   **Zygisk** must be enabled in your Magisk settings.
*   You must install the **PMPatch** Magisk Module to disable Android's signature verification check. Failure to do so will prevent the installation.
    *   **Download PMPatch:** [Here](https://github.com/vova7878-modules/PMPatch/releases/download/v1.2.0/PMPatch3.zip)

### Installation Steps

1.  **Install the APK:** Install the `Music-KM.apk` from the [Releases](https://github.com/YOUR_USERNAME/YOUR_REPOSITORY/releases) page as a simple update for the system music player.
2.  **Google Play Protect:** If Google Play Protect shows a warning, select "Install anyway."
3.  **Background Service Permissions:** Immediately after installation, the app will open system settings. You must grant permission for the **"Music-KM"** service to run. This is essential for monitoring the state of your chosen player.
4.  **Select Your Player:** On the next launch, the application will display a list of installed players. Select the player you wish to use.

### How to Change the Player

If you need to change the substitute player later:

1.  Go to your list of all applications.
2.  Launch the **"Music-KM Settings"** app.
3.  Select a different player from the list.

### Uninstallation

Since this is installed as a system component update, you cannot uninstall it directly from the launcher. Use one of the following methods:

**Option 1: Using ADB (Recommended)**
1.  Connect to your head unit via ADB.
2.  Run the command: `adb uninstall com.qf.musicplayer`
3.  Reboot your head unit.

**Option 2: Via App Settings**
1.  Go to Settings → Apps.
2.  Show system apps (usually via the three-dot menu).
3.  Find "Music-KM" in the list.
4.  Open it and select "Uninstall updates" from the three-dot menu.

---

## <a name="українська"></a>Опис українською

**Music-KM** — це допоміжний додаток для автомагнітол QF001 / Roco K706 (UIS 7862A/7862S), який дозволяє замінити стандартний музичний плеєр у системній медіа-каруселі на будь-який сторонній плеєр на ваш вибір (наприклад, Poweramp, Pulsar тощо).

### Як це працює

Цей додаток інтегрується в системний цикл кнопки "Mode":

1.  Він реєструє себе як системний додаток "Музика".
2.  Коли ви обираєте "Музика" за допомогою кнопки "Mode", Music-KM перехоплює команду.
3.  Потім він негайно запускає справжній музичний плеєр, який ви обрали в його налаштуваннях.
4.  Він "обманює" систему, накриваючи плеєр прозорим вікном і утримуючи фокус, щоб запобігти перемиканню медіа-каруселі.

Це створює відчуття повної інтеграції, ніби ваш улюблений плеєр є рідною частиною системи автомобіля.

### Вимоги до встановлення (Важливо!)

Щоб встановити цей додаток як системне оновлення та успішно обійти перевірку підпису, ви ПОВИННІ відповідати наступним вимогам:

*   Потрібен **Root-доступ**.
*   Має бути встановлений **Magisk**.
*   У налаштуваннях Magisk має бути увімкнений **Zygisk**.
*   Ви повинні встановити модуль Magisk **PMPatch**, щоб вимкнути системну перевірку підписів. Без нього встановлення буде неможливим.
    *   **Завантажити PMPatch:** [Тут](https://github.com/vova7878-modules/PMPatch/releases/download/v1.2.0/PMPatch3.zip)

### Кроки встановлення

1.  **Встановіть APK:** Встановіть файл `Music-KM.apk` зі сторінки [Релізів](https://github.com/YOUR_USERNAME/YOUR_REPOSITORY/releases) як звичайне оновлення для системного музичного плеєра.
2.  **Google Play Protect:** Якщо Google Play Protect покаже попередження, оберіть "Все одно встановити".
3.  **Дозволи для фонової служби:** Одразу після встановлення додаток відкриє системні налаштування. Ви повинні надати дозвіл на роботу службі **"Music-KM"**. Це необхідно для моніторингу стану обраного вами плеєра.
4.  **Виберіть ваш плеєр:** При наступному запуску додаток відобразить список встановлених плеєрів. Оберіть той, який ви хочете використовувати.

### Як змінити плеєр

Якщо вам потрібно буде пізніше змінити плеєр:

1.  Перейдіть до списку всіх ваших додатків.
2.  Запустіть додаток **"Налаштування Music-KM"**.
3.  Виберіть інший плеєр зі списку.

### Видалення

Оскільки додаток встановлюється як оновлення системного компонента, його не можна видалити напряму з лаунчера. Використовуйте один з наступних методів:

**Спосіб 1: Через ADB (рекомендовано)**
1.  Підключіться до вашої магнітоли через ADB.
2.  Виконайте команду: `adb uninstall com.qf.musicplayer`
3.  Перезавантажте магнітолу.

**Спосіб 2: Через налаштування додатків**
1.  Перейдіть у Налаштування → Додатки.
2.  Увімкніть показ системних додатків (зазвичай через меню з трьома крапками).
3.  Знайдіть "Music-KM" у списку.
4.  Відкрийте його та оберіть "Видалити оновлення" з меню (три крапки).

---

### Для розробників

Щоб ознайомитися з технічними деталями, особливостями реалізації та поясненням "хаків", використаних у цьому проекті, перегляньте [**посібник для розробників (README_DEV.md)**](README_DEV.md).
