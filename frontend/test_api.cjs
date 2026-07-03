const axios = require('axios');
async function test() {
    try {
        const res = await axios.get('http://localhost:8123/api/ai/love_app/chat/history', {
            headers: { Authorization: 'Bearer test' }
        });
        console.log(res.data);
    } catch (e) {
        console.error(e.response ? e.response.status + ' ' + e.message : e.message);
    }
}
test();
