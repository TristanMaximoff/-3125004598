package com.example;

//对应数据库java对象
import com.example.entity.Repair;
import com.example.entity.User;

//数据库操作接口，连接数据库和service层
import com.example.mapper.RepairMapper;
import com.example.mapper.UserMapper;

// service层
import com.example.service.RepairService;
import com.example.service.UserService;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);//读取控制台输入
    private static SqlSessionFactory factory;//连接数据库的方法
    private static User currentUser; //当前登录的用户

    public static void main(String[] args) throws Exception {   //throws Exception---免责申明
        // 初始化 MyBatis（连接数据库）
        //加载数据库配置文件
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis-config.xml"));

        while(true){
            System.out.println("===========================");
            System.out.println("      宿舍报修管理系统        ");
            System.out.println("===========================");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("3. 退出");
            System.out.print("请选择：");
            String op = sc.next();

            //自动管理sqlSession
            try (SqlSession session = factory.openSession(true)){
                UserMapper um = session.getMapper(UserMapper.class);
                //绑定SQL语句，后续通过um.()执行数据库操作
                //类比sc绑定控制台输入
                UserService us = new UserService(um);
                //用us和um将service层和mapper层连接

                if (op.equals("1")) login(us);
                else if (op.equals("2")) register(us);
                else if (op.equals("3")) break;
            }
        }
    }

    //注册功能
    //和service层的register不一样
    private static void register(UserService us){
        //1.收集用户信息
        System.out.print("角色 1-学生 2-管理员：");
        String type = sc.next();
        System.out.print("输入账号：");
        String account = sc.next();
        System.out.print("输入密码：");
        String pwd = sc.next();
        //type记录学生|管理员——不用int类型防止输入其他类型数据时程序出错
        //account记录账户
        //pwd记录密码
        //记录完毕后再下方调用相应方法存入User类型对象u中

        //2.将信息封装成临时对象
        User u = new User();
        u.setAccount(account);
        u.setPassword(pwd);
        u.setRole(type.equals("1") ? "student" : "admin");

        //3.调用userservice注册同时返回注册结果
        System.out.println(us.register(u));//
    }

    //登录功能
    private static void login(UserService us){
        //1.收集登录信息
        System.out.print("输入账号：");
        String account = sc.next();
        System.out.print("输入密码：");
        String pwd = sc.next();

        //2.将登录信息与数据库对照
        User user = us.login(account,pwd);
        if(user == null){
            System.out.println("登录失败");
            return;
        }

        currentUser = user;
        //3.将user信息传到login外面
        System.out.println("登录成功：" + (user.getRole().equals("student") ? "学生" : "管理员"));

        //4..学生第一次登录强制绑定宿舍
        if(user.getRole().equals("student")){
            //如果还没绑定宿舍，必须先绑才能进菜单
            if(currentUser.getDormStatus() == 0) {
                System.out.println("【首次登录，请先绑定宿舍！】");
                bindDorm(); //绑定宿舍
            }
            studentMenu(); //进入学生菜单
        }else{
            adminMenu(); //管理员直接进菜单
        }
    }

    //绑定宿舍
    private static void bindDorm() {
        //1.如果已经绑定过，直接不让操作
        //和login里面双重保护
        if(currentUser.getDormStatus() == 1){
            System.out.println("你已经绑定过宿舍，无法重复绑定！");
            return;
        }

        //2.录入宿舍信息
        System.out.print("输入楼栋方向（东/西）：");
        String direction = sc.next();
        System.out.print("输入楼号（1-10）：");
        int building = sc.nextInt();
        System.out.print("输入房间号（101-610）：");
        int room = sc.nextInt();

        //3.设置到用户对象
        currentUser.setDormDirection(direction);
        currentUser.setDormBuilding(building);
        currentUser.setDormRoom(room);

        //4.绑定完成状态改为1
        currentUser.setDormStatus(1);

        //5.更新到数据库
        //在while（true）外面的独立方法，所以要重新绑定SQL语句
        try (SqlSession session = factory.openSession(true)){
            UserService us = new UserService(session.getMapper(UserMapper.class));
            us.updateUser(currentUser);
        }

        System.out.println("宿舍绑定成功！");
    }

    //学生菜单
    private static void studentMenu(){
        try (SqlSession session = factory.openSession(true)){
            UserService us = new UserService(session.getMapper(UserMapper.class));
            RepairService rs = new RepairService(session.getMapper(RepairMapper.class));

            while(true){
                System.out.println("===== 学生菜单 =====");
                System.out.println("1.绑定宿舍 2.报修 3.我的记录 4.取消 5.改密码 6.退出");
                String op = sc.next();

                if(op.equals("1")) {
                    bindDorm();

                }else if(op.equals("2")){
                    //1.创建临时repair对象r
                    Repair r = new Repair();
                    //2.r录入数据
                    r.setStudentAccount(currentUser.getAccount());
                    System.out.print("设备类型：");
                    r.setDeviceType(sc.next());
                    System.out.print("描述：");
                    r.setDescription(sc.next());
                    r.setStatus("待处理");
                    rs.addRepair(r);
                    System.out.println("报修成功！");

                } else if (op.equals("3")) {
                    System.out.println("--- 我的报修记录 ---");
                    rs.myRepairs(currentUser.getAccount()).forEach(r->    //r指的是rs.myRepairs()传回来的集合里的每一个repair对象，都执行一遍括号内的操作
                            System.out.println("ID:" + r.getId() + " | 设备:" + r.getDeviceType() + " | 状态:" + r.getStatus())
                    );
                } else if (op.equals("4")) {
                    System.out.print("输入要取消的报修ID：");
                    rs.cancel(sc.nextInt());
                } else if (op.equals("5")) {
                    System.out.print("输入新密码：");
                    currentUser.setPassword(sc.next());
                    us.updateUser(currentUser);
                    System.out.println("密码修改成功！");
                } else break;
            }
        }
    }

    // 管理员菜单
    private static void adminMenu() {
        try (SqlSession session = factory.openSession(true)) {
            RepairService rs = new RepairService(session.getMapper(RepairMapper.class));
            while (true) {
                System.out.println("===== 管理员菜单 =====");
                System.out.println("1.全部报修 2.改状态 3.删除 4.退出");
                String op = sc.next();

                if (op.equals("1")) {
                    System.out.println("--- 全部报修记录 ---");
                    rs.allRepairs().forEach(r->
                            System.out.println("ID:" + r.getId() + " | 学生:" + r.getStudentAccount() + " | 状态:" + r.getStatus())
                    );
                } else if (op.equals("2")) {
                    System.out.print("输入报修ID：");
                    Integer id = sc.nextInt();
                    System.out.print("输入新状态（待处理/处理中/已完成）：");
                    rs.updateStatus(id, sc.next());
                } else if (op.equals("3")) {
                    System.out.print("输入要删除的报修ID：");
                    rs.delete(sc.nextInt());
                } else break;
            }
        }
    }
}