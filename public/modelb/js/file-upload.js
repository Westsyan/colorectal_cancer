(function ($) {
    'use strict';
    $(function () {
        $('.file-upload-browse').on('click', function () {
            var file = $(this).parent().parent().parent().find('.file-upload-default');
            file.trigger('click');
        });
        $('.file-upload-default').on('change', function () {
            if ($("input[name='name']").val() === "") {

                const name = $(this).val().replace(/C:\\fakepath\\/i, '');
                const nameNoSuffix = name.replace(".txt", "");
                $(this).parent().find('.form-control').val(name);
                $("input[name='name']").val(nameNoSuffix);
                $('#form').formValidation("revalidateField", "name");
            }

        });
    });
})(jQuery);