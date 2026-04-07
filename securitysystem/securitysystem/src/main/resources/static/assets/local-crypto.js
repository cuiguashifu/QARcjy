const LOCAL_CRYPTO_URL = 'http://127.0.0.1:18234';

async function checkLocalService() {
    try {
        const resp = await fetch(`${LOCAL_CRYPTO_URL}/health`, {
            method: 'GET',
            mode: 'cors'
        });
        if (resp.ok) {
            const data = await resp.json();
            return { available: true, status: data };
        }
        return { available: false, status: null };
    } catch (e) {
        return { available: false, error: e.message };
    }
}

async function encryptFile(fileContent, policy) {
    const base64Data = typeof fileContent === 'string' 
        ? fileContent 
        : await fileToBase64(fileContent);

    const resp = await fetch(`${LOCAL_CRYPTO_URL}/encrypt`, {
        method: 'POST',
        mode: 'cors',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            data: base64Data,
            policy: policy || 'role:user'
        })
    });

    if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.error || `加密失败: ${resp.status}`);
    }

    return await resp.json();
}

async function decryptFile(encryptedData, wrappedKey, policy) {
    const resp = await fetch(`${LOCAL_CRYPTO_URL}/decrypt`, {
        method: 'POST',
        mode: 'cors',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            encryptedData: encryptedData,
            wrappedKey: wrappedKey,
            policy: policy || ''
        })
    });

    if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.error || `解密失败: ${resp.status}`);
    }

    return await resp.json();
}

function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            const base64 = reader.result.split(',')[1];
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function base64ToBlob(base64, contentType) {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: contentType || 'application/octet-stream' });
}

function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

window.LocalCrypto = {
    checkService: checkLocalService,
    encrypt: encryptFile,
    decrypt: decryptFile,
    fileToBase64: fileToBase64,
    base64ToBlob: base64ToBlob,
    downloadBlob: downloadBlob,
    SERVICE_URL: LOCAL_CRYPTO_URL
};
