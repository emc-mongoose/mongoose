$(document).ready(function() {
    
    $("#stop").attr("disabled", "disabled");
    $(".distributed").hide();
    $(".data-node").hide();
    $(".driver").hide();
    $(".drivers-block").hide();
    $(".advanced-content").hide();
    
    $(".add-node").click(function() {
        if ($(".data-node").is(":visible")) {
            $(".data-node").hide();
        } else {
            $(".data-node").show();
        }
    });

    $(".add-driver").click(function() {
        if ($(".driver").is(":visible")) {
            $(".driver").hide();
        } else {
            $(".driver").show();
        }
    });

    $("#save").click(function() {
        $(".storages").append(appendStorage($("#data-node-text").val()));
        $("#data-node-text").val("");
        $(".data-node").hide();
    });

    $(document).on("click", ".remove", function() {
        $(this).parent().parent().remove();
    });

    function appendStorage(storage) {
        html = 
            '<div class="input-group">\
                    <span class="input-group-addon">\
                        <input type="checkbox">\
                    </span>\
                    <label class="form-control">' +
                        storage +
                    '</label>\
                    <span class="input-group-btn">\
                        <button type="button" class="btn btn-default remove">Remove</button>\
                    </span>\
            </div>';

        return html;
    }

    $("#save-driver").click(function() {
        $(".drivers").append(appendStorage($("#driver-text").val()));
        $("#driver-text").val("");
        $(".driver").hide();
    });

    function appendDriver(driver) {
        html = 
            '<div class="input-group">\
                    <span class="input-group-addon">\
                        <input type="checkbox">\
                    </span>\
                    <label class="form-control">' +
                        driver +
                    '</label>\
                    <span class="input-group-btn">\
                        <button type="button" class="btn btn-default remove">Remove</button>\
                    </span>\
            </div>';

        return html;
    }

    $("#distributed").click(function() {
        $(".distributed").show();
        $(".drivers-block").show();
    });

    $("#standalone").click(function() {
        $(".distributed").hide();
        $(".drivers-block").hide();
    });

    $("#more-settings").click(function() {
        if ($(".advanced-content").is(":visible")) {
            $(".advanced-content").hide();
        } else {
            $(".advanced-content").show();
        }
    });

    $("#start").click(function() {
        $.post("/start", {
            runTime: $("#dataNodes").val()
        },
        function(data,status){
            $("#start").attr("disabled", "disabled");
            $("#stop").removeAttr("disabled");
        });
    });

    $("#stop").click(function() {
        $(this).attr("disabled", "disabled");
        $("#start").removeAttr("disabled");
    });

    $("#loadTab a").click(function(e) {
    	e.preventDefault();
    	$("#loadHidden").val($(this).text());
    });

    $("#apiTab").click(function(e) {
    	e.preventDefault();
    	$("apiHidden").val($(this).text());
    });
});