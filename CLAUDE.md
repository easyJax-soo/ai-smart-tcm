# AI Smart Cloud 项目规范

## 技术栈

### 核心框架
- **JDK**: 21
- **Spring Boot**: 3.5.13

### 依赖版本
- Lombok: 1.18.36
- Hutool: 5.8.44
- Knife4j: 4.4.0

---

## 项目结构

## 开发规范

### OpenAPI 3 注解示例（Knife4j）

#### Controller 层注解
```java
@RestController
@RequestMapping("body")
@Tag(name = "body参数")
public class BodyController {

    @Operation(summary = "普通body请求")
    @PostMapping("/body")
    public ResponseEntity<FileResp> body(@RequestBody FileResp fileResp){
        return ResponseEntity.ok(fileResp);
    }

    @Operation(summary = "普通body请求+Param+Header+Path")
    @Parameters({
            @Parameter(name = "id", description = "文件id", in = ParameterIn.PATH),
            @Parameter(name = "token", description = "请求token", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "name", description = "文件名称", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/bodyParamHeaderPath/{id}")
    public ResponseEntity<FileResp> bodyParamHeaderPath(
            @PathVariable("id") String id,
            @RequestHeader("token") String token,
            @RequestParam("name") String name,
            @RequestBody FileResp fileResp){
        fileResp.setName(fileResp.getName() + ",receiveName:" + name + ",token:" + token + ",pathID:" + id);
        return ResponseEntity.ok(fileResp);
    }
}
```

#### 注解说明
| 注解 | 位置 | 说明 |
|------|------|------|
| `@Tag` | 类 | 分组名称 |
| `@Operation` | 方法 | 接口描述 |
| `@Parameter` | 方法/参数 | 参数描述，`in` 指定参数位置 |
| `ParameterIn.PATH` | - | 路径参数 |
| `ParameterIn.HEADER` | - | 请求头参数 |
| `ParameterIn.QUERY` | - | Query参数 |

#### 导入路径
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
```
