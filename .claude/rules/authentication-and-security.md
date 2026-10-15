# 認證與安全

## JWT（SmallRye JWT）

- 公私鑰位於 `src/main/resources/`
  - 公鑰：`publicKey.pem`
  - 私鑰：`privateKey.pem`
- 登入端點：`POST /login`
- 登入一律以 email 作為帳號；預設帳號：管理員 `admin@bestpartner.com.tw` / `admin`、一般使用者 `test@partmer.com.tw` / `user`
- RBAC 權限管理已啟用

## JWT 設定項目

| 設定 | 值 |
|-----|----|
| `mp.jwt.verify.issuer` | `bast-partner` |
| `mp.jwt.verify.publickey.location` | `publicKey.pem` |
| `smallrye.jwt.sign.key.location` | `privateKey.pem` |
| `jwt.refresh.switch` | `true`（可開關） |
