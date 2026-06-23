package com.snapmeal.order;
import com.snapmeal.auth.AuthInterceptor;import com.snapmeal.common.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;import javax.servlet.http.HttpServletRequest;import java.util.*;
@RestController @RequestMapping("/api/user/orders")
public class UserOrderController {
    private final JdbcTemplate jdbc;private final OrderService orders;public UserOrderController(JdbcTemplate jdbc,OrderService orders){this.jdbc=jdbc;this.orders=orders;}private long uid(HttpServletRequest r){return ((Number)r.getAttribute(AuthInterceptor.SUBJECT_ID)).longValue();}
    @PostMapping public ApiResponse<Map<String,Object>> submit(HttpServletRequest r,@RequestBody OrderService.SubmitRequest body){return ApiResponse.ok(orders.submit(uid(r),body));}
    @PostMapping("/{id}/pay") public ApiResponse<Map<String,Object>> pay(HttpServletRequest r,@PathVariable long id){return ApiResponse.ok(orders.pay(uid(r),id));}
    @GetMapping public ApiResponse<List<Map<String,Object>>> history(HttpServletRequest r,@RequestParam(required=false)Integer status){return ApiResponse.ok(status==null?jdbc.queryForList("select * from orders where user_id=? order by order_time desc",uid(r)):jdbc.queryForList("select * from orders where user_id=? and status=? order by order_time desc",uid(r),status));}
    @GetMapping("/{id}") public ApiResponse<Map<String,Object>> detail(HttpServletRequest r,@PathVariable long id){Integer count=jdbc.queryForObject("select count(*) from orders where id=? and user_id=?",Integer.class,id,uid(r));if(count==null||count==0)throw new BusinessException("订单不存在");return ApiResponse.ok(orders.detail(id));}
    @PostMapping("/{id}/cancel") public ApiResponse<Void> cancel(HttpServletRequest r,@PathVariable long id,@RequestBody(required=false)Reason reason){orders.cancel(uid(r),id,reason==null?"用户取消":reason.reason);return ApiResponse.ok();}
    @PostMapping("/{id}/reorder") public ApiResponse<Void> reorder(HttpServletRequest r,@PathVariable long id){orders.reorder(uid(r),id);return ApiResponse.ok();}
    public static class Reason{public String reason;}
}
