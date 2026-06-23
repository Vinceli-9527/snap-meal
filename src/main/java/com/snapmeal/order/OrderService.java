package com.snapmeal.order;

import com.snapmeal.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*; import java.sql.*; import java.time.*; import java.time.format.DateTimeFormatter; import java.util.*;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;
    @Value("${sky.shop.longitude:121.506377}") private double shopLongitude;
    @Value("${sky.shop.latitude:31.302272}") private double shopLatitude;
    @Value("${sky.shop.delivery-radius-km:5}") private double radius;
    public OrderService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Transactional
    public Map<String,Object> submit(long userId,SubmitRequest request){
        List<Map<String,Object>> cart=jdbc.queryForList("select * from shopping_cart where user_id=?",userId);
        if(cart.isEmpty())throw new BusinessException("购物车为空，无法下单");
        List<Map<String,Object>> addresses=jdbc.queryForList("select * from address_book where id=? and user_id=?",request.addressBookId,userId);
        if(addresses.isEmpty())throw new BusinessException("收货地址不存在"); Map<String,Object> address=addresses.get(0);
        if(Db.get(address,"longitude")!=null&&Db.get(address,"latitude")!=null){double km=distance(shopLatitude,shopLongitude,((Number)Db.get(address,"latitude")).doubleValue(),((Number)Db.get(address,"longitude")).doubleValue());if(km>radius)throw new BusinessException(String.format("收货地址距门店 %.1f 公里，超出 %.1f 公里配送范围",km,radius));}
        BigDecimal total=BigDecimal.ZERO;for(Map<String,Object> item:cart){BigDecimal price=new BigDecimal(String.valueOf(Db.get(item,"amount")));total=total.add(price.multiply(new BigDecimal(String.valueOf(Db.get(item,"number")))));}
        String number=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+String.format("%04d",new Random().nextInt(10000));
        KeyHolder keys=new GeneratedKeyHolder();BigDecimal finalTotal=total;
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("insert into orders(number,user_id,address_book_id,status,pay_status,pay_method,amount,remark,phone,address,consignee,estimated_delivery_time) values(?,?,?,1,0,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setString(1,number);ps.setLong(2,userId);ps.setLong(3,request.addressBookId);ps.setInt(4,request.payMethod);ps.setBigDecimal(5,finalTotal);ps.setString(6,request.remark);ps.setString(7,String.valueOf(Db.get(address,"phone")));ps.setString(8,addressText(address));ps.setString(9,String.valueOf(Db.get(address,"consignee")));ps.setTimestamp(10,Timestamp.valueOf(LocalDateTime.now().plusMinutes(40)));return ps;},keys);
        Number generatedId=null;
        if(keys.getKeys()!=null)generatedId=(Number)Db.get(keys.getKeys(),"id");
        if(generatedId==null)generatedId=keys.getKey();
        if(generatedId==null)throw new BusinessException("订单创建失败：未能获取订单编号");
        long orderId=generatedId.longValue();
        for(Map<String,Object> item:cart)jdbc.update("insert into order_detail(order_id,name,image,dish_id,setmeal_id,dish_flavor,number,amount) values(?,?,?,?,?,?,?,?)",orderId,Db.get(item,"name"),Db.get(item,"image"),Db.get(item,"dish_id"),Db.get(item,"setmeal_id"),Db.get(item,"dish_flavor"),Db.get(item,"number"),Db.get(item,"amount"));
        jdbc.update("delete from shopping_cart where user_id=?",userId);Map<String,Object> out=new LinkedHashMap<>();out.put("id",orderId);out.put("number",number);out.put("amount",total);out.put("status",1);return out;
    }

    @Transactional public Map<String,Object> pay(long userId,long orderId){int changed=jdbc.update("update orders set pay_status=1,status=2,checkout_time=current_timestamp where id=? and user_id=? and status=1",orderId,userId);if(changed==0)throw new BusinessException("订单不存在、已支付或状态不允许支付");Map<String,Object> out=new LinkedHashMap<>();out.put("orderId",orderId);out.put("paymentMode","mock-wechat");out.put("paid",true);out.put("transactionId","MOCK"+System.currentTimeMillis());return out;}
    @Transactional public void cancel(long userId,long orderId,String reason){int changed=jdbc.update("update orders set status=6,cancel_reason=? where id=? and user_id=? and status in (1,2)",reason,orderId,userId);if(changed==0)throw new BusinessException("当前订单不能取消");}
    @Transactional public void reorder(long userId,long orderId){List<Map<String,Object>> details=jdbc.queryForList("select * from order_detail where order_id=? and exists(select 1 from orders where id=? and user_id=?)",orderId,orderId,userId);if(details.isEmpty())throw new BusinessException("订单不存在");for(Map<String,Object>d:details)jdbc.update("insert into shopping_cart(user_id,dish_id,setmeal_id,dish_flavor,name,image,amount,number) values(?,?,?,?,?,?,?,?)",userId,Db.get(d,"dish_id"),Db.get(d,"setmeal_id"),Db.get(d,"dish_flavor"),Db.get(d,"name"),Db.get(d,"image"),Db.get(d,"amount"),Db.get(d,"number"));}
    public Map<String,Object> detail(long orderId){List<Map<String,Object>> rows=jdbc.queryForList("select * from orders where id=?",orderId);if(rows.isEmpty())throw new BusinessException("订单不存在");Map<String,Object> out=new LinkedHashMap<>(rows.get(0));out.put("details",jdbc.queryForList("select * from order_detail where order_id=?",orderId));return out;}
    private String addressText(Map<String,Object>a){return value(a,"PROVINCE_NAME")+value(a,"CITY_NAME")+value(a,"DISTRICT_NAME")+value(a,"DETAIL");}
    private String value(Map<String,Object>a,String k){return Db.get(a,k)==null?"":String.valueOf(Db.get(a,k));}
    private double distance(double lat1,double lon1,double lat2,double lon2){double p=Math.PI/180;double x=0.5-Math.cos((lat2-lat1)*p)/2+Math.cos(lat1*p)*Math.cos(lat2*p)*(1-Math.cos((lon2-lon1)*p))/2;return 12742*Math.asin(Math.sqrt(x));}
    public static class SubmitRequest {public long addressBookId;public int payMethod=1;public String remark;}
}
