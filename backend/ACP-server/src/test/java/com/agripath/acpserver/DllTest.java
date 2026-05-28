package com.agripath.acpserver;

import org.apache.tomcat.jni.Library;

public class DllTest {
    public interface AlgorithmLibrary extends Library {
        // 自动加载 algorithm.dll（Windows）或 libalgorithm.so（Linux）
        AlgorithmLibrary INSTANCE = Native.load("algorithm", AlgorithmLibrary.class);

        Pointer createAlgorithm();
        int algorithm_add(Pointer alg, int a, int b);
        String algorithm_processString(Pointer alg, String input);
        void destroyAlgorithm(Pointer alg);
    }

    public static void main(String[] args) {
        Pointer alg = AlgorithmLibrary.INSTANCE.createAlgorithm();

        int sum = AlgorithmLibrary.INSTANCE.algorithm_add(alg, 10, 30);
        System.out.println("计算结果：" + sum);

        String str = AlgorithmLibrary.INSTANCE.algorithm_processString(alg, "Windows 测试");
        System.out.println(str);

        AlgorithmLibrary.INSTANCE.destroyAlgorithm(alg);
    }
}
