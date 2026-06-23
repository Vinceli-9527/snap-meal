package com.snapmeal.order;
import com.snapmeal.common.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final JdbcTemplate jdbc;private final OrderService orders;public AdminOrderController(JdbcTemplate jdbc,OrderService orders){this.jdbc=jdbc;this.orders=orders;}
    @GetMapping public ApiResponse<List<Map<String,Object>>> list(@RequestParam(required=false)Integer status,@RequestParam(required=false)String number){StringBuilder sql=new StringBuilder("select * from orders where 1=1");List<Object>a=new ArrayList<>();if(status!=null){sql.append(" and status=?");a.add(status);}if(number!=null&&!number.trim().isEmpty()){sql.append(" and number like ?");a.add("%"+number+"%");}sql.append(" order by order_time desc");return ApiResponse.ok(jdbc.queryForList(sql.toString(),a.toArray()));}
    @GetMapping("/statistics") public ApiResponse<List<Map<String,Object>>> statistics(){return ApiResponse.ok(jdbc.queryForList("select status,count(*) count from orders group by status order by status"));}
    @GetMapping("/{id}") public ApiResponse<Map<String,Object>> detail(@PathVariable long id){return ApiResponse.ok(orders.detail(id));}
    @PostMapping("/{id}/confirm") public ApiResponse<Void> confirm(@PathVariable long id){move(id,2,3,"只有待接单订单可以接单");return ApiResponse.ok();}
    @PostMapping("/{id}/deliver") public ApiResponse<Void> deliver(@PathVariable long id){move(id,3,4,"只有已接单订单可以派送");return ApiResponse.ok();}
    @PostMapping("/{id}/complete") public ApiResponse<Void> complete(@PathVariable long id){move(id,4,5,"只有派送中订单可以完成");jdbc.update("update orders set delivery_time=current_timestamp where id=?",id);return ApiResponse.ok();}
    @PostMapping("/{id}/reject") public ApiResponse<Void> reject(@PathVariable long id,@RequestBody Reason r){int n=jdbc.update("update orders set status=6,rejection_reason=? where id=? and status=2",r.reason,id);if(n==0)throw new BusinessException("只有待接单订单可以拒单");return ApiResponse.ok();}
    @PostMapping("/{id}/cancel") public ApiResponse<Void> cancel(@PathVariable long id,@RequestBody Reason r){int n=jdbc.update("update orders set status=6,cancel_reason=? where id=? and status in (2,3)",r.reason,id);if(n==0)throw new BusinessException("当前订单不能取消");return ApiResponse.ok();}
    private void move(long id,int from,int to,String message){if(jdbc.update("update orders set status=? where id=? and status=?",to,id,from)==0)throw new BusinessException(message);}
    public static class Reason{public String reason;}
}
