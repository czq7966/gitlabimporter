// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package nd.sdp.gerrit.plugins.gitlabimporter.client;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.core.client.JavaScriptObject;

import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.Plugin;


import java.util.ArrayList;
import java.util.Collection;

import com.google.gerrit.client.info.GeneralPreferences;

class ImportScreen extends HorizontalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      if (Plugin.get().isSignedIn()){	
        String title = "友情提醒：Gerrit功能开通后，请关闭Gitlab的Push功能，只将代码Push到Gerrit上，以免造成数据冲突或丢失！";
        screen.setPageTitle(title);
	screen.show(new ImportScreen());
      }
      else {
        screen.setPageTitle("请先登录");
      }
	
    }
  }


  ImportScreen() {
    setStyleName("autom-panel");
    display();
  }

  public static class ProjectInfo extends JavaScriptObject {
    public final native String appName() /*-{ return this.app_name; }-*/;
    protected ProjectInfo() {
    }
  };


  private static class ProjectInput extends JavaScriptObject {
    static ProjectInput create() {
      return (ProjectInput) createObject();
    }

    protected ProjectInput() {
    }

    final native void setMessage(String n) /*-{ if(n)this.message=n; }-*/;
  }

  public static class ProjectOutput extends JavaScriptObject {
    public final native boolean result() /*-{ return this.result; }-*/;
    public final native String message() /*-{ return this.message; }-*/;

    protected ProjectOutput() {
    }
  }
	
  private void startImport(final String value) {
	ProjectInput info = ProjectInput.create();
	info.setMessage(value);
        new RestApi("projects").id("All-Projects").view("import-project")
            .post(info, new AsyncCallback<ProjectOutput>() {
              @Override
              public void onSuccess(ProjectOutput result) {		
		UIBlocker.unblock(ImportScreen.this);	
		if (result.result()) {
//		  Plugin.get().go("/admin/projects/"+value);
		  Plugin.get().go("/admin/projects/"+value+",access");
                }
                else {
                  Plugin.get().refresh();
                  Window.alert(result.message());	
		}
              }

              @Override
              public void onFailure(Throwable caught) {
                Plugin.get().refresh();
		UIBlocker.unblock(ImportScreen.this);
                Window.alert("RPC调用出现异常！");
              }
            });

  }

	// flexible table for displaying projects from gitlab.
	private FlexTable gitProjs = null;

	/**
	 * 展示sdp数据
	 */
	void display() {
		// 数据table已经存在的情况下，刷新数据需要先移除先前的组件
		if (gitProjs != null) {
			remove(gitProjs);
		}
		gitProjs = new FlexTable();
		gitProjs.setText(0, 0, "No.");
		gitProjs.setText(0, 1, "Git Repository Address");
		gitProjs.setText(0, 2, "Operation");
		// 获取数据
		new RestApi("projects").id("All-Projects").view("import-project")
		    .get(new AsyncCallback<JsArray<ProjectInfo>>() {

		      @Override
		      public void onSuccess(JsArray<ProjectInfo> result) {
			if (result != null && result.length() > 0) {
				ProjectInfo info = null; 
				for (int row = 0; row < result.length(); row++) {
					info = result.get(row);
					String repoName = info.appName();
					gitProjs.setText(row + 1, 0, Integer.toString(row));
					gitProjs.setText(row + 1, 1, repoName);
					gitProjs.setWidget(row + 1, 2, createMigrateButton(repoName));
				}
			} else {
				gitProjs.setText(1, 0, "No data was found.");
				gitProjs.getFlexCellFormatter().setColSpan(1, 0, 3);
			}
		      }

		      @Override
		      public void onFailure(Throwable caught) {
			Window.alert("RPC调用出现异常");
		      }
		    });


		gitProjs.setBorderWidth(0);
		gitProjs.setCellSpacing(0);
		/* 追加选中行时的样式 */
		gitProjs.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent e) {
				Cell c = gitProjs.getCellForEvent(e);
				if(c != null) {
					/* 清除画面已有的选中行*/
					Element selRepo = Document.get().getElementById("autom-repo-selected");
					if(selRepo != null) {
						selRepo.removeAttribute("id");
					}
					/* 选中当前行 */
					c.getElement().getParentElement().setId("autom-repo-selected");
				}
			}
			
		});
		add(gitProjs);

	}



	/**
	 * 创建迁移[:migrate]按钮
	 * */
	private Button createMigrateButton(final String repoName) {
		Button btn = new Button("开通");
		btn.addStyleName("autom-button");
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				// 阻断画面
				UIBlocker.block(ImportScreen.this, "正在开通 "+repoName+"，请不要关闭窗口....");
				startImport(repoName);

			}
		});
		btn.setEnabled(true);
		return btn;
	}
}
