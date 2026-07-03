import { getToken } from './src/utils/auth.js';
import axios from 'axios';

async function test() {
    try {
        const res = await axios.get('http://localhost:8123/api/ai/love_app/chat/history', {
            headers: {
                Authorization: 'Bearer admin_mock_token' // Hardcoding or will fail, let's just make a direct local fetch without auth file dependency for a quick check.
            }
        });
        console.log(res.data);
    } catch (e) {
        console.error(e.response ? e.response.status : e.message);
    }
}
test();
