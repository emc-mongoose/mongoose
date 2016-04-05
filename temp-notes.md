- WebUiRunner().run() запускается из ModeDispatcher
- Выполняются настройки jettyServer, в том числе указывется расположение web.xml, запускается server
- Открывается index.html с подключенными стилями, двумя блоками (div) cover - экран загрузки и app (???) и script tag для requirejs
- requirejs script tag вызывает main.js, в requirejs/config лежат ссылки на другие js-библиотеки
- В main.js через jquery отправляется get запрос на '/main' и через MainServlet получаются appConfig
- var config = appConfig.config
- Далее вызывается mainController.run(config)
- В run(config) вызывается метод render(config),
который в свою очередь вызывает renderNavbar() и
renderTabs(),
в которых компилируются соответствующие Handlebars
- Далее вызывается confMenuController.run(props, runIdArray)
- Внутри cMC.run() создается объект run c полем mode,
вызывается render(), в котором компилируется confMenuTemplate
- После этого выполняются baseConfController.setup(props) и
extendedConfController.setup(props), setDefautRunMode(run.mode)

***
Для input-field компилируется новый шаблон (extendedConf.hbs)
- Необходимо посмотреть класс ".activate" в этих полях (зачем?)

***

!!!Для построения дерева из конфиг.параметров не нужно(!!!) value параметра
Поэтому хотя параметр - это объект, а файл в дереве сценариев нет, то, т.к. value параметра не
имеет значения в html коде элементы будут одни и те же. Таким образом массив файлов сценариев
анологичен объекту с несколькими парами ключ-значение, если нам не важно значение