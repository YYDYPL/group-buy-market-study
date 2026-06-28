package com.hjs.study.api;

import com.hjs.study.api.response.Response;

public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);

}
