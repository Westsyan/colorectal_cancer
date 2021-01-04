function createCode() {
    let code = "";
    let random = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
    for (let i = 0; i < 4; i++) {
        let index = Math.floor(Math.random() * 10);
        code += random[index];
    }
    $("#code").val(code);
}

function loginFormValidation() {
    $('#form').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: '用户名不能为空!'
                    }
                }
            },
            pwd: {
                validators: {
                    notEmpty: {
                        message: "请输入密码！"
                    }
                }
            },
            code: {
                validators: {
                    callback: {
                        message: '验证码错误！',
                        callback: function(code) {
                            let inputCode =$("#code").val(); //取得输入的验证码并转化为大写
                            return inputCode === code;
                        }
                    },
                }
            },
        }
    });
}

function loginFormValidationEn() {
    $('#form').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: 'Account cannot be empty!'
                    }
                }
            },
            pwd: {
                validators: {
                    notEmpty: {
                        message: "Password cannot be empty！"
                    }
                }
            },
            code: {
                validators: {
                    callback: {
                        message: 'Valid code error!',
                        callback: function(code) {
                            let inputCode =$("#code").val(); //取得输入的验证码并转化为大写
                            return inputCode === code;
                        }
                    },
                }
            },
        }
    });
}

function registerFormValidation() {
    $('#form').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: '用户名不能为空!'
                    },
                    remote: {
                        type: 'GET',
                        url: '/colorectal/user/checkAccount',
                        message:`用户名已存在！<a style='color:#2D8B67' href='/colorectal/user/loginPage'>点击去登录</a>`,
                        delay: 1000
                    },
                }
            },
            pwd: {
                validators: {
                    notEmpty: {
                        message: "请输入密码！"
                    },
                    stringLength: {
                        min: 6,
                        message:"最少输入6位数密码！"
                    },
                }
            },
            rPwd:{
                validators: {
                    identical:{
                        field: 'pwd',
                        message:'两次输入密码不一致'
                    }
                }
            },
            code: {
                validators: {
                    callback: {
                        message: '验证码错误！',
                        callback: function(code) {
                            let inputCode =$("#code").val(); //取得输入的验证码并转化为大写
                            return inputCode === code;
                        }
                    },
                }
            },
        }
    });
}

function registerFormValidationEn() {
    $('#form').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: 'Account cannot be empty!'
                    },
                    remote: {
                        type: 'GET',
                        url: '/colorectal/user/checkAccount',
                        message:`Account already exist! <a style='color:#2D8B67' href='/colorectal/en/user/loginPage'>Click to sign in</a>`,
                        delay: 1000
                    },
                }
            },
            pwd: {
                validators: {
                    notEmpty: {
                        message: "Password cannot be empty！"
                    },
                    stringLength: {
                        min: 6,
                        message:"Enter at least 6 digits password！"
                    },
                }
            },
            rPwd:{
                validators: {
                    identical:{
                        field: 'pwd',
                        message:'The two passwords do not agree!'
                    }
                }
            },
            code: {
                validators: {
                    callback: {
                        message: 'Valid code error!',
                        callback: function(code) {
                            let inputCode =$("#code").val(); //取得输入的验证码并转化为大写
                            return inputCode === code;
                        }
                    },
                }
            },
        }
    });
}
