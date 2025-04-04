import axios from 'axios';

const BASE = '/jmeter/test';

export default {
    getCatalog: () => {
        return axios.get(`${BASE}/catalog`);
    },
};