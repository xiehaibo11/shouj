import { useState, useEffect } from 'react';
import { getTrafficStats } from '../services/api';

export function useTraffic() {
  const [upload, setUpload] = useState(0);
  const [download, setDownload] = useState(0);
  const [uploadSpeed, setUploadSpeed] = useState(0);
  const [downloadSpeed, setDownloadSpeed] = useState(0);

  useEffect(() => {
    let lastUpload = 0;
    let lastDownload = 0;
    let lastTime = Date.now();

    const updateTraffic = async () => {
      try {
        const stats = await getTrafficStats();
        const now = Date.now();
        const timeDiff = (now - lastTime) / 1000;

        setUpload(stats.upload || 0);
        setDownload(stats.download || 0);

        if (timeDiff > 0) {
          setUploadSpeed(((stats.upload || 0) - lastUpload) / timeDiff);
          setDownloadSpeed(((stats.download || 0) - lastDownload) / timeDiff);
        }

        lastUpload = stats.upload || 0;
        lastDownload = stats.download || 0;
        lastTime = now;
      } catch (error) {
        console.error('获取流量数据失败:', error);
      }
    };

    updateTraffic();
    const interval = setInterval(updateTraffic, 1000);

    return () => clearInterval(interval);
  }, []);

  return {
    upload,
    download,
    uploadSpeed,
    downloadSpeed,
  };
}

