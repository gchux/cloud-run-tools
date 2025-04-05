import axios from 'axios';

const BASE = '/jmeter/test';

export default {
    getCatalog: (catalog: string = "default") => {
        return axios.get(`${BASE}/catalog/${catalog}`);
    },
};