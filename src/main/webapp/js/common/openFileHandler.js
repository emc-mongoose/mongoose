/**
 * Created on 19.04.16.
 */
define([], function () {

	var fullFileName = '';
	var onLoadAction = function (json) {
	};

	const reader = new FileReader();
	reader.onload = function (data) {
		content = data.target.result;
		const json = JSON.parse(content);
		onLoadAction(json, fullFileName);
	};
	reader.onerror = function (data) {
		console.error("File couldn't be read. Code" + data.target.error.code);
	};

	function openFileEvent(data) {
		const files = data.target.files; // FileList object
		const file = files[0];
		fullFileName = file.name;
		reader.readAsText(file);
	}

	function setFileReaderOnLoadAction(action) {
		onLoadAction = action;
	}

	return {
		event: openFileEvent,
		setFileReaderOnLoadAction: setFileReaderOnLoadAction
	}
});