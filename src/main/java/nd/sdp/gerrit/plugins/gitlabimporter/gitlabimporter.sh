
####默认参数
#_old_gerrit_server=${_old_gerrit_server:-sdpgerrit-admin}
#_remote_server=${_remote_server:-sdpgitlab}

#_local_server=${_local_server:-gerrit-server}
#_local_server_admin=${_local_server_admin:-gerrit-admin}
#_local_gerrit_site=${_local_gerrit_site:-/home/gerrit/site}
##_local_server=${_local_server:-gerrit-server-debug}
##_local_server_admin=${_local_server_admin:-gerrit-admin-debug}
##_local_gerrit_site=${_local_gerrit_site:-/home/nd/gerrit}

#_local_git_dir=${_local_git_dir:-${_local_gerrit_site}/git}
_local_bak_dir=${_local_bak_dir:-${_local_gerrit_site}/bak}


_local_admin_group="Administrators"
_local_non_interactive_group="Non-Interactive Users"
_local_predefined_group=("${_local_admin_group}" "${_local_non_interactive_group}")

_local_group_subfix_owner="_owner"
_local_group_subfix_read="_read"
_local_group_subfix_push="_push"
_local_group_subfix_review="_review"
_local_group_subfix_verified="_verified"
_local_group_subfix_submit="_submit"
_local_project_gropus_subfix="${_local_group_subfix_owner} ${_local_group_subfix_read} ${_local_group_subfix_push} ${_local_group_subfix_review} ${_local_group_subfix_verified} ${_local_group_subfix_submit}"
_local_project_group_description=("项目的所有者" "有下载代码的权限" "有上传代码的权限" "有审核代码的权限" "有验证代码的权限" "有提交代码入库的权限")



##########################函数####################




########################################## 系统相关 ########################################

# 刷新缓存
function gerrit_flush_caches()
{
	__name=${1:---all}
	__result=`ssh ${_local_server_admin} "gerrit flush-caches ${__name}"`
	echo $__result
}





########################################## 项目相关 ########################################

# 判断项目是否存在 $1=项目名
function gerrit_exist_project()
{
	__name=${1%.git}
	__dir="${_local_git_dir}/${__name}.git/objects"
	__result=`ssh ${_local_server} test -d "${__dir}" || echo 0`
	if [ "$__result" == "0" ]; then
		echo 0
	else
		echo 1
	fi
}

# 判断远程项目是否存在 $1=项目名
function gerrit_remote_exist_project()
{
	__name=${1%.git}.git
	__fatal="fatal:"
	__result=`ssh ${_local_server} git ls-remote ${_remote_server}:${__name} 2>&1`
	__result=$(echo $__result | grep "${__fatal}")
	if [ "$__result" == "" ]; then
		echo 1
	else
		echo 0
	fi

}

# 创建项目 $1=项目名
function gerrit_create_project()
{
	__name=${1%.git}
	__result=$(gerrit_exist_project ${__name})
	if [ "${__result}" == "1" ]; then
		echo 0 "项目已存在：${__name}"
	else
		__result=`ssh ${_local_server_admin} "gerrit create-project \"${__name}\""`
		__result=$(gerrit_exist_project ${__name})
		__=$(gerrit_flush_caches)
		echo $__result
	fi
}



# 从远程服上镜像项目 $1=项目名
function gerrit_mirror_project_from_remote()
{
	__name=${1%.git}
	__result=($(gerrit_exist_project ${__name}))	
	if [ "${__result}" == "1" ]; then
		echo 0 "本地项目已存在：${__name}"
	else
			__remote_project=${__name}.git
			__local_project=${_local_git_dir}/${__name}.git
			__result=$(gerrit_remote_exist_project "${__name}")
			if [ "${__result}" == "0" ]; then
				echo 0 "远程项目不存在：${__name}"				
			else
				__msg=`ssh ${_local_server_admin} "gerrit create-project \"${__name}.git\""`
				__msg=`ssh ${_local_server} "cd \"${__local_project}\"; git fetch ${_remote_server}:${__remote_project} +refs/heads/*:refs/heads/* +refs/tags/*:refs/tags/*"`
				__result=($(gerrit_exist_project ${__name}))
				__=$(gerrit_flush_caches)
				echo "$__result" "$__msg"
			fi
	fi
}

# 从远程服上更新项目 $1=项目名
function gerrit_update_project_from_remote()
{
	__name=${1%.git}
	__result=($(gerrit_exist_project ${__name}))	
	if [ "${__result}" != "1" ]; then
		echo 0 "本地项目不存在：${__name}"
	else
			__remote_project=${__name}.git
			__local_project=${_local_git_dir}/${__name}.git
			ssh ${_local_server} "cd ${__local_project} ; git fetch ${_remote_server}:${__remote_project}  +refs/heads/*:refs/heads/* +refs/tags/*:refs/tags/*"
			__=$(gerrit_flush_caches)
	fi
}






########################################## 组相关 ########################################
# 判断组是否存在 $1=组名
function gerrit_exist_group()
{
	__name=$1
	__result=`ssh ${_local_server_admin} "gerrit ls-groups -q \"${__name}\"'" `
	if [ "$__result" == "${__name}" ]; then
		echo 1
	else
		echo 0
	fi 
}


# 创建组 $1＝组名 $2=所有者 $3=描述
function gerrit_create_group()
{
	__name=$1
	__owner=$2
	__description=$3
	
#	__owner=${__owner:-"--owner Administrators"}
	__description=${__description:-"${__name}"}


	if [ "$__owner" != "" ]; then
		__owner=" --owner \"${__owner}\""
	fi

	if [ "${__name}" != "" ]; then
		ssh ${_local_server_admin} "gerrit create-group ${__owner} --description \"${__description}\" \"${__name}\""
	fi
	__result=$(gerrit_exist_group $__name)
	echo $__result
}

# 设置组的用户成员
function gerrit_set_members_user()
{
	__name=$1
	__user=$2

	if [ "${__name}" != "" -a "${__user}" != ""  ]; then
		ssh ${_local_server_admin} "gerrit set-members --add \"${__user}\" \"${__name}\""
		echo 1
	else
		echo 0
	fi

}


# 设置组的组成员
function gerrit_set_members_group()
{
	__name=$1
	__group=$2

	if [ "${__name}" != "" -a "${__group}" != "" ]; then
		ssh ${_local_server_admin} "gerrit set-members --include \"${__group}\" \"${__name}\""
		echo 1
	else
		echo 0
	fi

}

# 获取组的UUID
function gerrit_get_group_uuid()
{
	__name=$1
	__uuid=
	__exist=$(gerrit_exist_group "$__name")
	if [ "$__exist" == "1" ]; then
		__group=(`ssh ${_local_server_admin} "gerrit ls-groups -v -q \"${__name}\"'" `)
		__uuid=${__group[1]}
	fi
	echo "$__uuid"
}





# 根据项目批量创建6个组 $1＝项目名
function gerrit_batch_create_group_by_project()
{
	__name=${1%.git}
	__groups=$_local_project_gropus_subfix
	__group=	
	__group_owner=
	__group_desc=
	__result=0
	__i=0
	if [ "${__name}" != "" ]; then
		for v in $__groups
		do			
			__group=${__name}$v
			__group_desc=${_local_project_group_description[$__i]}"（$__name 项目）"
			__result=$(gerrit_create_group "$__group" "$__group_owner" "$__group_desc")
			__group_owner=${__name}${_local_group_subfix_owner}	
			__i=${__i}+1
		done

	fi 
	__=$(gerrit_flush_caches)
	echo $__result
}


# 根据项目将其它５组加入read组 $1＝项目名
function gerrit_batch_include_group_by_project()
{
	__name=${1%.git}
	__group_read=${__name}${_local_group_subfix_read}
	__groups=$_local_project_gropus_subfix
	__group=	
	__result=0
	__i=0
	if [ "${__name}" != "" ]; then
		for v in $__groups
		do			
			if [ "${v}" != "${_local_group_subfix_read}" ]; then 
				__group=${__name}$v
				__result=$(gerrit_exist_group "$__group")
				if [ "${__result}" == "1" ]; then 
					__result=$(gerrit_set_members_group "$__group_read" "$__group")
				fi
			fi
		done

	fi 
	echo $__result
}






###########################################################################################################



# 从远程服上镜像项目并初始化权限
# 命令行参数
# _remote_server 	远程Gitlab的SSH连接
# _local_server		Gerrit的SSH连接
# _local_server_admin	Gerrit的管理员SSH连接
# _local_gerrit_site	Gerrit的安装目录
# _local_git_dir	Gerrit的Git仓库存放目录
# _import_projet	要从Gitlab上导入的项目库名称
# _current_user         当前用户，用于设置其为owner角色
function plugin_gerrit_import_project()	
{
	__name=${_import_projet%.git}
	__result=($(gerrit_mirror_project_from_remote "$__name"))
	__result2=""
	__result3=""
	if [ "$__result" == "1" ]; then
		__result=($(gerrit_batch_create_group_by_project "$__name"))
		if [ "$__result" == "1" ]; then
			__=($(gerrit_batch_include_group_by_project "$__name"))
		fi
		__result2=""
		if [ "${__result}" == "1" -a "${_current_user}" != "" ]; then
			__group=${__name}${_local_group_subfix_owner}
			__result2=$(gerrit_set_members_user "${__group}" "${_current_user}")
			if [ "${__result2}" == "0" ]; then
				__result2="设置用户 ${_current_user} 为 ${__group} 角色失败！"
			fi
		fi
	fi
	echo "${__result[*]} ${__result2} ${__result3}　"
}


# 执行从命令行传入的语句
eval $@




