package com.wallet.unhandled_exception.controller;

import com.wallet.unhandled_exception.model.UserInfo;
import com.wallet.unhandled_exception.service.SSOAuthUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private SSOAuthUserServiceImpl ssoAuthUserService;

    @RequestMapping(value = "/{email}", method = RequestMethod.GET)
    public UserInfo getUserPublicInfo(@PathVariable String email) {

        return ssoAuthUserService.getUserPublicProfile(email);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public UserInfo getUserDetails(@RequestParam String ssoToken) {
        return ssoAuthUserService.getUserDetailsIfTokenValid(ssoToken);
    }
}
