package cn.com.fmsh.util;

import java.util.Objects;

public class ApduUtil {

    // 卡片操作相关变量
    private static String CLA;
    private static String INS;
    private static String P1;
    private static String P2;
    private static String LC;
    private static String DATE;
    private static String LE;
    private static String StrApdu = "";
    private static String ErrMessage;

    /**
     * 生成Seed
     * @param key1 用户输入的支付密码，长度任意
     * @param key2 运营商密码  16字节 明文
     */
    public static String CreatSeed(String key,String k2) {
        CLA = "80";
        INS = "E0";
        P1 = "00";
        P2 = "00";
        LC = "22";
        DATE =k2+ ShaUtil.getSha1(key).substring(0, 32)
                + CRCUtil.getCrc16(k2+ShaUtil.getSha1(key).substring(0, 32));
        LE = "44";
        StrApdu = CLA + INS + P1 + P2 + LC + DATE + LE;
        return StrApdu;
    }

    /**
     * 获取公钥
     *
     * @param coinType  `00`表示比特币；`3C`表示以太坊
     * @param accountID 钱包ID 4字节
     */
    public static String getPublicKey(String coinType, String accountID) {
        CLA = "80";
        INS = "E2";
        P1 = "00";
        P2 = coinType; // `00`表示比特币；`3C`表示以太坊
        LC = "04";
        DATE = accountID;
        LE = "44";
        StrApdu = CLA + INS + P1 + P2 + LC + DATE + LE;
        return StrApdu;
    }

    /**
     * 签名
     *
     * @param coinType        `00`表示?比特币；`3C`表示以太坊
     * @param accountID       钱包ID
     * @param hashTransaction 待签名的Hash值
     */
    public static String getSignature(String coinType, String accountID,
                                      String hashTransaction) {
        CLA = "80";
        INS = "E4";
        P1 = "00";
        P2 = coinType; // `00`表示?比特币；`3C`表示以太坊
        LC = "24";
        DATE = accountID + hashTransaction;
        LE = "44";
        StrApdu = CLA + INS + P1 + P2 + LC + DATE + LE;
        return StrApdu;

    }

    /**
     * seed恢复
     *
     * @param key            用户支付密码
     * @param seedCiphertext seed密文
     */
    public static String recoveryKey(String key, String seedCiphertext) {
        CLA = "80";
        INS = "E6";
        P1 = "00";
        P2 = "00";
        LC = "50";
        DATE = key + seedCiphertext;
        LE = "04";
        StrApdu = CLA + INS + P1 + P2 + LC + DATE + LE;
        return StrApdu;

    }

    public static String ErrMessage(String Sw) {
        if (Sw.equals("6E 00")) ErrMessage = "CLA不合法";
        if (Sw.equals("6D 00")) ErrMessage = "INS不合法";
        if (Sw.equals("6A 86")) ErrMessage = "p1，p2参数不合法";
        if (Sw.equals("67 00")) ErrMessage = "Lc长度不正确";
        if (Sw.equals("69 85")) ErrMessage = "卡片SEED已存在";
        if (Sw.equals("69 88")) ErrMessage = "私钥不存在";
        if (ErrMessage != null) {
            return ErrMessage;
        } else {
            return Sw;
        }
    }
}
