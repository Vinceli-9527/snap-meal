const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { saving: false },
  onLoad() {
    auth.requireLogin();
  },
  submit(event) {
    const form = event.detail.value;
    if (!form.consignee || !form.phone || !form.detail) {
      wx.showToast({ title: '请填写完整地址', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    api.post('/api/user/addresses', {
      consignee: form.consignee,
      sex: '',
      phone: form.phone,
      provinceName: '上海市',
      cityName: form.cityName || '上海',
      districtName: form.districtName || '杨浦',
      detail: form.detail,
      label: '家',
      longitude: 121.506377,
      latitude: 31.302272
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      wx.navigateBack();
    }).catch(() => {})
      .finally(() => this.setData({ saving: false }));
  }
});
