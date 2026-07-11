<template>
	<div>
		<el-card>
			<div slot="header">
				<span>修改当前登录账户</span>
			</div>
			<el-form v-if="isAdmin" :model="account" label-width="50px">
				<el-form-item label="账号">
					<el-input v-model="account.username"></el-input>
				</el-form-item>
				<el-form-item label="密码">
					<el-input v-model="account.password"></el-input>
				</el-form-item>
				<el-popconfirm title="确定修改吗？" icon="el-icon-user-solid" iconColor="#409EFF" @onConfirm="save">
					<el-button type="primary" size="medium" icon="el-icon-check" slot="reference" :disabled="!account.username || !account.password">确认修改</el-button>
				</el-popconfirm>
			</el-form>
			<div v-else>
				<el-form label-width="80px">
					<el-form-item label="用户名">
						<el-input :value="profile.username" disabled></el-input>
					</el-form-item>
					<el-form-item label="昵称">
						<el-input v-model="profile.nickname" maxlength="30" show-word-limit></el-input>
					</el-form-item>
					<el-button type="primary" @click="saveNickname" :disabled="!profile.nickname">保存昵称</el-button>
				</el-form>
				<el-divider></el-divider>
				<el-form label-width="80px">
					<el-form-item label="旧密码">
						<el-input v-model="password.oldPassword" show-password></el-input>
					</el-form-item>
					<el-form-item label="新密码">
						<el-input v-model="password.newPassword" show-password></el-input>
					</el-form-item>
					<el-button type="primary" @click="savePassword" :disabled="!password.oldPassword || password.newPassword.length < 6">修改密码</el-button>
				</el-form>
			</div>
		</el-card>
	</div>
</template>

<script>
import {changeAccount, getPlayerProfile, changeNickname, changePassword} from "@/api/account";

export default {
	name: "Setting",
	data() {
		return {
			user: {},
			account: {
				username: '',
				password: ''
			},
			profile: {username: '', nickname: ''},
			password: {oldPassword: '', newPassword: ''}
		}
	},
	computed: {
		isAdmin() {
			return this.user.role === 'ROLE_admin'
		}
	},
	created() {
		this.user = JSON.parse(window.localStorage.getItem('user') || null)
		this.account.username = this.user.username
		if (!this.isAdmin) {
			getPlayerProfile().then(res => { this.profile = res.data })
		}
	},
	methods: {
		save() {
			changeAccount(this.account).then(res => {
				this.msgSuccess(res.msg)
				this.logout()
			})
		},
		logout() {
			window.localStorage.removeItem('token')
			window.localStorage.removeItem('user')
			this.$router.push('/login')
		},
		saveNickname() {
			changeNickname(this.profile.nickname).then(res => {
				this.msgSuccess(res.msg)
				this.user.nickname = this.profile.nickname
				window.localStorage.setItem('user', JSON.stringify(this.user))
			})
		},
		savePassword() {
			changePassword(this.password.oldPassword, this.password.newPassword).then(res => {
				this.msgSuccess(res.msg)
				this.logout()
			})
		}
	}
}
</script>

<style scoped>
.el-card {
	width: 50%;
}
</style>
