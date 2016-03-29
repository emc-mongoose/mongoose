- WebUiRunner(AppConfig) запускается из ModeDispatcher
- Выполняются настройки jettyServer, в том числе указывется расположение web.xml, appConfig добавляется в webAppContext, запускается server
- Открывается index.html с подключенными стилями, двумя блоками (div) cover - экран загрузки и app (???) и script tag для requirejs
- requirejs script tag вызывает main.js, в requirejs/config лежат ссылки на другие js-библиотеки
- В main.js через jquery отправляется get запрос на '/main' и через MainServlet получаются appConfig
- var props = appConfig.properties
- Далее вызывается mainController.run(props)
- В run(props) вызывается метод render(props),
который в свою очередь вызывает renderNavbar() и
renderTabs(),
в которых компилируются соответствующие Handlebars
- Далее вызывается confMenuController.run(props, runIdArray)
- Внутри cMC.run() создается объект run c полем mode,
вызывается render(), в котором компилируется confMenuTemplate
- После этого выполняются baseConfController.setup(props) и
extendedConfController.setup(props), setDefautRunMode(run.mode)

***

Classes that need the attention
ScenarioRunner
JsonScenario

***

- Кнопка open в текущей версии - просто input\[type="file"\] +
- Она должна выглядеть, как остальные кнопки, +
- При первом запуске должен выводиться tooltip -
- input\[type="text\] должен быть "резиновым"
- Логика загрузки файла при нажатии на кнопку содержится в файле confMenuController