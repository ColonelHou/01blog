# SpringAOPʵ��ԭ��Proxy + chain of responsibility��
#### proxy�����������ͨ��JDK��̬�������ʵ�֡�
#### ������ģʽ
���Դ��룺
```
public class AdvisorChainTest {
	
	public static void main(String args[]){
		//��Ҫ�����Ŀ�����
		UserService user = new UserServiceImpl();
		//����������
		UserService proxy = (UserService)ProxyBuilder.buildProxy(UserService.class,user);
		
		//ͨ������������Ŀ�귽��
		proxy.insert(null);
	}
	//ProxyBuilder��װ��JDK��̬�������ɴ������Ĺ���
	static class ProxyBuilder{
		public static Object buildProxy(Class<?> interfaces, Object target){
			
			return Proxy.newProxyInstance(ProxyBuilder.class.getClassLoader(), 
					new Class<?>[]{interfaces},  new DefaultInvocationHandler(target));
		}
	}
}
```
�����
> insert user into db

> INFO  edu.ouc.chain.TimeInterceptor - insert execute time:0ms.

�����������Ĺ����������޸ģ�����insert�������ء�
```
public class InterceptorChainFactory {

	public List<Object> getInterceptorList(){
		List<Object> interceptorList = new ArrayList<>();
		interceptorList.add(new TimeInterceptor());
		Set<String> exclusions = new HashSet<String>();
		exclusions.add("test");
		exclusions.add("insert");//������Ҫ���ط�����
		interceptorList.add(new MethodFilterInterceptor(exclusions));
		return interceptorList;
	}
}
```
�����
> Exception in thread "main" edu.ouc.chain.MethodFilterInterceptor$MethodNotAllowedException: method insert is not allowed!
