const BuildHtml = class {

    buildHtml(json) {
        let html = "";
        json.forEach(j => {
            if (j.type === "taskname") {
                html += this.taskname()
            } else if (j.type === "text") {
                html += this.text(j)
            } else if (j.type === "radio") {
                html += this.radio(j)
            } else if (j.type === "file") {
                html += this.file(j)
            } else if(j.type === "runButton"){
                html += this.runButton()
            }
        })
        return html
    }

    taskname() {
        return `<div class="col-md-12">
                    <div class="form-group row">
                        <label class="col-sm-3 col-form-label">任务名:</label>
                        <div class="col-sm-4">
                            <input class="form-control" name="name" id="name">
                        </div>
                    </div>
                </div>`
    }

    text(json) {
        return `<div class="form-group row" id="${json.id}">
                    <div class="reDraw">
                        <label class="col-sm-3 col-form-label">${json.title}：</label>
                        <div class="col-sm-6">
                            <input type="text" class="form-control" name="${json.name}" id="${json.name}" placeholder="${json.placeholder}" value="${json.value}">
                        </div>
                    </div>
                </div>`
    }

    radio(json) {
        let options = "";
        $.each(json.options,(value, option) => {
            let checked = "";
            if (value === json.checked) {
                checked =`checked="checked"`
            }
            options += `<label class="col-form-label label-radio"> <input type="radio"  name="${json.name}" 
                            value="${json.value}" onclick="${json.onclick}" ${checked} /> ${option}</label>`
        })
        return `<div class="col-md-12" id="${json.name}">
                    <div class="form-group row">
                        <label class="col-sm-3 col-form-label">${json.title}：</label>
                        <div class="col-sm-4">
                            ${options}
                        </div>
                    </div>
                </div>`
    }

    file(json) {
        return `<div class="col-md-12" id="${json.name}">
                    <div class="form-group row">
                        <label class="col-sm-3 col-form-label">${json.title}:</label>
                        <div class="col-sm-4">
                            <input type="file" name="${json.name}" class="file-upload-default" style="width: 500px">
                                <div class="input-group col-xs-12">
                                    <input type="text" class="form-control file-upload-info" disabled placeholder="${json.title}">
                                    <span class="input-group-append">
                                        <button class="file-upload-browse btn btn-primary" type="button">
                                            选择</button>
                                    </span>
                                </div>
                            <span class="example"><a href="/colorectal/utils/downloadExample?file=${json.example}"><em id="egCrics">
                                示例文件</em></a>
                            </span>
                        </div>
                    </div>
                </div>`
    }

    runButton() {
        return `<div class="col-md-12">
                    <div class="form-group row">
                        <div class="col-sm-3"></div>
                        <button type="button" class="btn btn-primary" style="width: 200px" onclick="Run()">
                                            运行</button>
                    </div>
                </div>`
    }

}


