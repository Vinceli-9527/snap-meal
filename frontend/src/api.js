export async function readJson(response) {
  return response.json().catch(() => ({
    success: false,
    message: '服务器返回了无法识别的数据'
  }));
}

export async function requestJson(path, options = {}) {
  let response;
  try {
    response = await fetch(path, options);
  } catch (error) {
    throw new Error('无法连接服务器，请确认服务已启动');
  }

  const result = await readJson(response);
  if (!response.ok || !result.success) {
    const error = new Error(result.message || '请求处理失败');
    error.status = response.status;
    throw error;
  }
  return result.data;
}

export function jsonOptions(options = {}, headers = {}) {
  return {
    ...options,
    headers: {
      ...headers,
      ...(options.headers || {}),
      'Content-Type': 'application/json'
    }
  };
}
