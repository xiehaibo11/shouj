import React from 'react';
import { View, ScrollView, StyleSheet } from 'react-native';
import { Card, Title, Paragraph, Chip, Divider } from 'react-native-paper';
import { useRoute } from '@react-navigation/native';

export default function ProxyDetailScreen() {
  const route = useRoute();
  const { proxyName } = route.params || {};

  // TODO: 从API获取代理详情

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <Title>基本信息</Title>
          <Divider style={styles.divider} />
          <View style={styles.infoRow}>
            <Paragraph style={styles.label}>名称:</Paragraph>
            <Paragraph style={styles.value}>{proxyName}</Paragraph>
          </View>
          <View style={styles.infoRow}>
            <Paragraph style={styles.label}>类型:</Paragraph>
            <Chip mode="outlined" compact>Shadowsocks</Chip>
          </View>
          <View style={styles.infoRow}>
            <Paragraph style={styles.label}>延迟:</Paragraph>
            <Chip mode="outlined" compact icon="speedometer">125ms</Chip>
          </View>
        </Card.Content>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  card: {
    marginBottom: 16,
  },
  divider: {
    marginVertical: 12,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  label: {
    fontWeight: 'bold',
  },
  value: {
    color: 'gray',
  },
});

