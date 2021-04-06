function GetState(value, row) {
    if (row.state === "2") {
        return `<div class="failed">失败</div><input class='state' value='${row.state}'>`
    } else if (row.state === "1") {
        return `<div class="success">成功</div><input class='state' value='${row.state}'>`
    } else {
        return `<div>运行中 <img src='/assets/images/timg.gif'  style='width: 20px; height: 20px;'></div><input class='state' value='${row.state}'>`;
    }
}

function progressHandlingFunction(e,time) {
    if (e.lengthComputable) {
        let progress = $('#progress');
        progress.attr({value: e.loaded, max: e.total}); //更新数据到进度条
        let time1 = (new Date()).getTime();
        let speed = (e.loaded / 1024) / ((time1 - time) / 1000);
        let percent = e.loaded / e.total * 100;
        progress.html(percent.toFixed(2) + "%   " + parseInt(speed) + "KB/S");
        progress.css('width', percent.toFixed(2) + "%");
    }
}

const MyTable = class {
    deleteByUrl(url) {
        swal({
                title: "",
                text: "确定删除？\n\n",
                type: "warning",
                showCancelButton: true,
                showConfirmButton: true,
                confirmButtonClass: "btn-danger",
                confirmButtonText: "确定",
                closeOnConfirm: false,//设置为false才可以触发双重swal
                cancelButtonClass: "btn-outline-danger",
                cancelButtonText: "取消"
            },
            function () {
                let index = layer.load(1, {
                    shade: [0.1, '#fff']
                });
                $.ajax({
                    url: url,
                    type: "delete",
                    success: function (data) {
                        layer.close(index);
                        if (data.code === 400) {
                            swal("删除失败", data.msg, "error")
                        } else {
                            swal("", "\n删除成功！\n", "success")
                            UpdateTable();
                        }
                    }
                });
            });
    }

    openLog(url,title="日志") {
        $.post( url, (data)=> {
                layer.open({
                    type: 1,
                    title: title,
                    skin: 'layui-layer-rim', //加上边框
                    area: ['800px', '600px'],
                    shadeClose: true, //点击遮罩关闭
                    content: `<div style="padding: 10px">${data}</div>`
                });
            }
        )
    }

    openLayerPage(url,title="日志") {
        $.post( url, (data)=> {
                layer.open({
                    type: 1,
                    title: title,
                    skin: 'layui-layer-rim', //加上边框
                    area: ['800px', '600px'],
                    shadeClose: true, //点击遮罩关闭
                    content: `<div style="padding: 10px">${data}</div>`
                });
            }
        )
    }

    getTaskName(suffix,nameId="#name",formId="#form") {
        let date = new Date();
        let year = date.getFullYear();
        let month = date.getMonth() + 1;
        let day = date.getDate();
        let h = date.getHours();
        let m = date.getMinutes();
        let s = date.getSeconds();
        let name = "" + year + month + day + h + m + s + "_" + suffix;
        $(nameId).val(name);
        $(formId).formValidation("revalidateField", "name");
    }

    runningIcon() {
        let element = "<div id='content'><span id='info'>运行中... </span></div>";
        return layer.msg(element, {
            icon: 16
            , shade: 0.01,
            time: 20000000
        });
    }

    processIcon(){
        let element = "<div id='content'><span id='info'>文件上传中： </span><span id='progress'></span></div>";
        return layer.msg(element, {
            icon: 16
            , shade: 0.01,
            time: 20000000
        });
    }

}

