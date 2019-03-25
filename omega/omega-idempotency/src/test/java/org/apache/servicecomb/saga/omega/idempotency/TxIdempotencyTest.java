package org.apache.servicecomb.saga.omega.idempotency;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

/**
 * @author: takumiCX
 * @create: 2019-03-24
 * @email: takumicx@zju.edu.cn
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IdempotencyTestMain.class,IdempotencyConfig.class})
@AutoConfigureMockMvc
public class TxIdempotencyTest {

    @Autowired
    private TxIdempotencyRepository txIdempotencyRepository;


    private static final String globalTxId = UUID.randomUUID().toString();
    private final String newLocalTxId = UUID.randomUUID().toString();
    private final String anotherLocalTxId = UUID.randomUUID().toString();


    @Before
    public void testSetUp(){
        TxIdempotency txIdempotency = new TxIdempotency();
        txIdempotency.setCreatedAt(new Date());
        txIdempotency.setUpdatedAt(new Date());
        txIdempotency.setServiceName("ServiceName");
        txIdempotency.setInstanceId("InstanceId");
        txIdempotency.setGlobalTxId(globalTxId);
        txIdempotency.setParentTxId(anotherLocalTxId);
        txIdempotency.setState("Running");
        TxIdempotency save = txIdempotencyRepository.save(txIdempotency);

        System.out.println(save);
    }

    @Test
    public void testSave(){


    }


    @Test
    public void testFindByGlobalTxId(){

        TxIdempotency globalTxId = txIdempotencyRepository.findByGlobalTxId(TxIdempotencyTest.globalTxId);

        System.out.println(globalTxId);

    }




}
