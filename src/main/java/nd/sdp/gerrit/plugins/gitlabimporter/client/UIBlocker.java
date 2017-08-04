package nd.sdp.gerrit.plugins.gitlabimporter.client;

import java.lang.reflect.Method;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Panel;

/**
 * 阻断前端画面的div-layer组件
 * @since 2017/07/26
 * */
public class UIBlocker {
	private final static String UIBLOCKER_CLAZZNAME = "autom-ui-blocker";
	private final static String UIBLOCKER_ID = "autom-ui-blocker";
	private final static DivElement blocker = Document.get().createDivElement();
	static{
		blocker.setClassName(UIBLOCKER_CLAZZNAME);
		blocker.setId(UIBLOCKER_ID);
	}
	
	/**
	 * @param p 要阻断的目标panel
	 * */
	public static  void block(Panel p){
		p.getElement().appendChild(blocker);
	}

	/**
	 * @param p 要解除阻断的目标panel
	 * */
	public static  void unblock(Panel p){
		p.getElement().removeChild(blocker);
	}
	
}
