package org.apache.servicecomb.saga.omega.idempotency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author: takumiCX
 * @create: 2019-03-24
 * @email: takumicx@zju.edu.cn
 **/

@SpringBootApplication
public class IdempotencyTestMain {

    public static void main(String[] args) {

        SpringApplication.run(IdempotencyTestMain.class, args);

    }
}
