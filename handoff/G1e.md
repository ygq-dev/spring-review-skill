任务编号：G.1e
完成内容：补 5 条 AST 检测器 + 13 个新单测。
- DAO-04：@Transactional 方法体内调 RestTemplate/WebClient/feignClient。
- WEB-04：allowedOrigins("*") + allowCredentials(true) 同文件出现。
- SEC-04：csrf().disable() 或 CsrfConfigurer.disable()。
- RES-01：资源类型局部变量未在 try-with-resources 中且未 .close()。
- OBS-01：log.info("x" + var) 形式的拼接。
- 覆盖 15 条规则。
关键决定：
- RES-01 覆盖 14 种常见资源类型（InputStream/OutputStream/Reader/Writer/Connection/Statement 等）。
- OBS-01 只检测 SLF4J 风格的 log/logger 变量名；不检测自定义门面。
- WEB-04 采用"同文件两调用共现"的启发式；不追踪 call chain。
- SEC-04 检查 scope 里含 csrf/Csrf 的 disable 调用。
- DAO-04 检查 scope 里含 restTemplate/webClient/feignClient 的调用。
未决问题：
- RES-01 对 try-with-resources 多重变量声明支持（已在 wrapped 里收全部）。
- RES-01 未检测"包装资源"（如 FilterInputStream 包裹）的传递。
- WEB-04 未解析 CorsConfiguration bean 的代码结构。
- OBS-01 未检测非字面量的拼接如 String.format。
- DAO-04 未检测 HTTP 客户端接口的抽象层（如 HttpClient 接口）。
下一步建议：
- 阶段 G 收尾：G 冻结清单 + handoff/G.md。
- 或补更多检测器（TEST-01 真实 DB、STYLE-01 命名、MIG-01 javax 已由正则覆盖）。