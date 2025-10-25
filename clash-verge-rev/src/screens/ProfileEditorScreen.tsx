import React, { useState } from 'react';
import { View, ScrollView, StyleSheet } from 'react-native';
import { TextInput, Button, useTheme } from 'react-native-paper';
import { useRoute, useNavigation } from '@react-navigation/native';

export default function ProfileEditorScreen() {
  const theme = useTheme();
  const navigation = useNavigation();
  const route = useRoute();
  const { profileId } = route.params || {};

  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [updateInterval, setUpdateInterval] = useState('0');

  const handleSave = async () => {
    // TODO: 保存配置
    navigation.goBack();
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <TextInput
        label="配置名称"
        value={name}
        onChangeText={setName}
        mode="outlined"
        style={styles.input}
      />
      <TextInput
        label="订阅链接"
        value={url}
        onChangeText={setUrl}
        mode="outlined"
        style={styles.input}
        placeholder="https://..."
        autoCapitalize="none"
        autoCorrect={false}
      />
      <TextInput
        label="自动更新间隔（分钟）"
        value={updateInterval}
        onChangeText={setUpdateInterval}
        mode="outlined"
        style={styles.input}
        keyboardType="numeric"
        placeholder="0 表示禁用自动更新"
      />
      <Button
        mode="contained"
        onPress={handleSave}
        style={styles.button}
      >
        保存
      </Button>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: 16,
  },
  input: {
    marginBottom: 16,
  },
  button: {
    marginTop: 16,
  },
});

