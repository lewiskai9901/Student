===== 短信验证码 + 小红书粉丝查询 - 部署指南 =====

【一键部署 (推荐)】
  上传整个目录到服务器后执行:
  sudo bash deploy.sh

【服务器要求】
- Linux (x64/arm64)
- 至少 1GB 内存 (Puppeteer + Chrome 需要)
- 开放端口: 3333 (短信工具) + 3457 (粉丝查询)

【包含两个服务】
1. sms-tool-server.js  (端口 3333) - 短信验证码工具
2. xhs-fans-server.js  (端口 3457) - 小红书粉丝查询 (Puppeteer)

【手动部署步骤】

1. 上传文件到服务器:
   scp -r sms-tool-deploy/ root@你的服务器IP:/opt/sms-tool/

2. SSH 登录服务器, 进入目录:
   cd /opt/sms-tool

3. 安装 Chrome (粉丝查询需要):
   # Ubuntu/Debian:
   apt install -y google-chrome-stable
   # 或安装 chromium:
   apt install -y chromium-browser

4. 安装依赖:
   npm install

5. 启动服务:
   # 方式一 - pm2 (推荐):
   pm2 start sms-tool-server.js --name sms-tool
   pm2 start xhs-fans-server.js --name xhs-fans
   pm2 save && pm2 startup

   # 方式二 - nohup:
   nohup node sms-tool-server.js > sms-tool.log 2>&1 &
   nohup node xhs-fans-server.js > xhs-fans.log 2>&1 &

6. 自定义端口:
   PORT=8080 node sms-tool-server.js
   PORT=8081 node xhs-fans-server.js

7. 云服务器安全组: 放行 3333 和 3457 端口

【访问】
  短信工具:    http://服务器IP:3333
  粉丝查询:    http://服务器IP:3457
  粉丝管理后台: http://服务器IP:3457/admin

【文件说明】
  sms-tool-server.js    - 短信工具服务端
  xhs-fans-server.js    - 粉丝查询服务端 (Puppeteer 无头浏览器)
  sms-code-tool.html    - 前端主页面
  sms-admin.html        - 短信管理后台
  xhs-fans-checker.html - 粉丝查询独立页面
  deploy.sh             - 一键部署脚本
  sms-tool-data/        - 短信数据目录 (自动创建)
  .xhs-data/            - 小红书Cookie数据 (自动创建)
