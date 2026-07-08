import { LogIn } from 'lucide-react';
import { FormEvent, useState } from 'react';

interface LoginPanelProps {
  isLoading: boolean;
  errorMessage: string | null;
  onSubmit: (credentials: { studentIdentity: string; password: string; rememberMe: boolean }) => Promise<void>;
}

export function LoginPanel({ isLoading, errorMessage, onSubmit }: LoginPanelProps) {
  const [studentIdentity, setStudentIdentity] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit({ studentIdentity, password, rememberMe });
  }

  return (
    <section className="login-panel" aria-labelledby="login-title">
      <div>
        <h2 id="login-title">队员登录</h2>
        <p>welcome to custacmwiki</p>
      </div>
      <form onSubmit={handleSubmit}>
        <label>
          学号姓名
          <input
            autoComplete="username"
            onChange={(event) => setStudentIdentity(event.target.value)}
            placeholder="输入学号姓名"
            required
            value={studentIdentity}
          />
        </label>
        <label>
          密码
          <input
            autoComplete="current-password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="输入密码"
            required
            type="password"
            value={password}
          />
        </label>
        <label className="checkbox-field login-remember-field">
          <span>记住我一个月</span>
          <input
            checked={rememberMe}
            onChange={(event) => setRememberMe(event.target.checked)}
            type="checkbox"
          />
        </label>
        <button className="primary-button" disabled={isLoading} type="submit">
          <LogIn size={16} aria-hidden="true" />
          {isLoading ? '登录中' : '登录'}
        </button>
      </form>
      {errorMessage ? <p className="form-error" role="alert">{errorMessage}</p> : null}
    </section>
  );
}
