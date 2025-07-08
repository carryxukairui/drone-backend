package com.demo.dronebackend.util;


import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.demo.dronebackend.config.AliSmsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SmsService {
    private final AliSmsConfig aliSmsConfig;

    private final StringRedisTemplate redisTemplate;
    private static final String SMS_CODE_PREFIX = "SMS_CODE_";

    private static final String SECRET_KEY = "drone_code_secret_key";
    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(aliSmsConfig.getAccessKeyId())
                .setAccessKeySecret(aliSmsConfig.getAccessKeySecret())
                .setEndpoint("dysmsapi.aliyuncs.com");
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    public int sendSms(String phone,String sign) {
        String expectedSign = phone + SECRET_KEY;
        if(!expectedSign.equals(phone+sign)){
            return -1;
        }
        String code = String.valueOf(new Random().nextInt(900000)+100000);
        try {
            com.aliyun.dysmsapi20170525.Client client = createClient();
            SendSmsRequest request = new SendSmsRequest()
                    .setSignName(aliSmsConfig.getSignName())
                    .setTemplateCode(aliSmsConfig.getTemplateCode())
                    .setPhoneNumbers(phone)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            client.sendSms(request);

            // 存入 Redis，有效期 2 分钟
            redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, 2, TimeUnit.MINUTES);
            return 1;
        } catch (Exception e) {

            throw new RuntimeException("短信发送异常");
        }
    }
    /**
     * 获取存储的验证码
     */
    public String getStoredCode(String phone) {
        return redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
    }

    /**
     * 删除验证码（成功登录后调用）
     */
    public void deleteCode(String phone) {
        redisTemplate.delete(SMS_CODE_PREFIX + phone);
    }
}
