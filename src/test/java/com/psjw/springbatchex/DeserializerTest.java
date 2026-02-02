package com.psjw.springbatchex;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Map;

public class DeserializerTest {
    @Test
    public  void Deserializer() throws Exception {
        // BATCH_STEP_EXECUTION_CONTEXT 데이터
        String context = "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAEdAARYmF0Y2gudGFza2xldFR5cGV0AD1vcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC5pdGVtLkNodW5rT3JpZW50ZWRUYXNrbGV0dAANYmF0Y2gudmVyc2lvbnQABTUuMi40dAAOYmF0Y2guc3RlcFR5cGV0ADdvcmcuc3ByaW5nZnJhbWV3b3JrLmJhdGNoLmNvcmUuc3RlcC50YXNrbGV0LlRhc2tsZXRTdGVwdAAHZmlyc3RJZHNyAA5qYXZhLmxhbmcuTG9uZzuL5JDMjyPfAgABSgAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAAAABF3g=";

        System.out.println("--- Step Execution Context ---");
        deserializeAndPrint(context);
    }

    private static void deserializeAndPrint(String serializedContext) throws Exception {
        //base64 decoding -> java deserialize
        final ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(serializedContext));
        final ObjectInputStream objectInputStream = new ObjectInputStream(in);
        Map<String, Object> contextMap = (Map<String, Object>) objectInputStream.readObject();
        contextMap.forEach((k, v) -> System.out.println(k + " = " + v));
    }
}
