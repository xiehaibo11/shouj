import React, { useState } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  IconButton,
  FAB,
  Menu,
  Chip,
  useTheme,
  Portal,
  Dialog,
  TextInput,
  Button,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import { useNavigation } from '@react-navigation/native';

import { useProfiles } from '../hooks/useProfiles';
import { formatDate } from '../utils/format';

export default function ProfilesScreen() {
  const theme = useTheme();
  const navigation = useNavigation();
  const [menuVisible, setMenuVisible] = useState<string | null>(null);
  const [dialogVisible, setDialogVisible] = useState(false);
  const [subscriptionUrl, setSubscriptionUrl] = useState('');

  const {
    profiles,
    currentProfile,
    selectProfile,
    updateProfile,
    deleteProfile,
    createProfile,
  } = useProfiles();

  const handleSelectProfile = async (uid: string) => {
    try {
      await selectProfile(uid);
    } catch (error) {
      Alert.alert('错误', '切换配置失败');
    }
  };

  const handleUpdateProfile = async (uid: string) => {
    try {
      await updateProfile(uid);
      Alert.alert('成功', '配置更新成功');
    } catch (error) {
      Alert.alert('错误', '配置更新失败');
    }
  };

  const handleDeleteProfile = (uid: string, name: string) => {
    Alert.alert(
      '确认删除',
      `确定要删除配置 "${name}" 吗？`,
      [
        { text: '取消', style: 'cancel' },
        {
          text: '删除',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteProfile(uid);
            } catch (error) {
              Alert.alert('错误', '删除失败');
            }
          },
        },
      ]
    );
  };

  const handleAddSubscription = async () => {
    if (!subscriptionUrl.trim()) {
      Alert.alert('错误', '请输入订阅链接');
      return;
    }

    try {
      await createProfile({
        type: 'remote',
        url: subscriptionUrl,
      });
      setDialogVisible(false);
      setSubscriptionUrl('');
      Alert.alert('成功', '订阅添加成功');
    } catch (error) {
      Alert.alert('错误', '订阅添加失败');
    }
  };

  const renderProfileItem = ({ item }: { item: any }) => {
    const isActive = currentProfile?.uid === item.uid;
    const isUpdating = item.updating;

    return (
      <TouchableOpacity
        onPress={() => handleSelectProfile(item.uid)}
        onLongPress={() => setMenuVisible(item.uid)}
      >
        <Card
          style={[
            styles.profileCard,
            isActive && { borderColor: theme.colors.primary, borderWidth: 2 },
          ]}
        >
          <Card.Content>
            <View style={styles.profileHeader}>
              <View style={styles.profileInfo}>
                <Icon
                  name={item.type === 'remote' ? 'cloud-download' : 'file'}
                  size={24}
                  color={isActive ? theme.colors.primary : 'gray'}
                />
                <View style={styles.profileDetails}>
                  <Title style={styles.profileName}>{item.name || '未命名'}</Title>
                  {item.type === 'remote' && (
                    <Paragraph style={styles.profileUrl} numberOfLines={1}>
                      {item.url}
                    </Paragraph>
                  )}
                  <View style={styles.profileMeta}>
                    {item.updated && (
                      <Chip
                        mode="outlined"
                        compact
                        style={styles.chip}
                        icon="clock"
                      >
                        {formatDate(item.updated)}
                      </Chip>
                    )}
                    {isActive && (
                      <Chip
                        mode="flat"
                        compact
                        style={[
                          styles.chip,
                          { backgroundColor: theme.colors.primary },
                        ]}
                        textStyle={{ color: 'white' }}
                      >
                        使用中
                      </Chip>
                    )}
                  </View>
                </View>
              </View>
              <Menu
                visible={menuVisible === item.uid}
                onDismiss={() => setMenuVisible(null)}
                anchor={
                  <IconButton
                    icon="dots-vertical"
                    onPress={() => setMenuVisible(item.uid)}
                  />
                }
              >
                {item.type === 'remote' && (
                  <Menu.Item
                    leadingIcon="refresh"
                    onPress={() => {
                      setMenuVisible(null);
                      handleUpdateProfile(item.uid);
                    }}
                    title="更新"
                  />
                )}
                <Menu.Item
                  leadingIcon="pencil"
                  onPress={() => {
                    setMenuVisible(null);
                    navigation.navigate('ProfileEditor', {
                      profileId: item.uid,
                    });
                  }}
                  title="编辑"
                />
                <Menu.Item
                  leadingIcon="delete"
                  onPress={() => {
                    setMenuVisible(null);
                    handleDeleteProfile(item.uid, item.name);
                  }}
                  title="删除"
                />
              </Menu>
            </View>
          </Card.Content>
        </Card>
      </TouchableOpacity>
    );
  };

  return (
    <View style={styles.container}>
      <FlatList
        data={profiles}
        renderItem={renderProfileItem}
        keyExtractor={item => item.uid}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="file-outline" size={64} color="gray" />
            <Paragraph style={styles.emptyText}>暂无配置文件</Paragraph>
            <Button
              mode="contained"
              onPress={() => setDialogVisible(true)}
              style={styles.emptyButton}
            >
              添加订阅
            </Button>
          </View>
        }
      />

      <FAB
        icon="plus"
        style={styles.fab}
        onPress={() => setDialogVisible(true)}
      />

      {/* 添加订阅对话框 */}
      <Portal>
        <Dialog
          visible={dialogVisible}
          onDismiss={() => setDialogVisible(false)}
        >
          <Dialog.Title>添加订阅</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label="订阅链接"
              value={subscriptionUrl}
              onChangeText={setSubscriptionUrl}
              mode="outlined"
              placeholder="https://..."
              autoCapitalize="none"
              autoCorrect={false}
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogVisible(false)}>取消</Button>
            <Button onPress={handleAddSubscription}>确定</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  listContent: {
    padding: 16,
  },
  profileCard: {
    marginBottom: 16,
    elevation: 2,
  },
  profileHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  profileInfo: {
    flexDirection: 'row',
    flex: 1,
  },
  profileDetails: {
    flex: 1,
    marginLeft: 12,
  },
  profileName: {
    fontSize: 16,
    marginBottom: 4,
  },
  profileUrl: {
    fontSize: 12,
    color: 'gray',
    marginBottom: 8,
  },
  profileMeta: {
    flexDirection: 'row',
    gap: 8,
  },
  chip: {
    height: 28,
  },
  fab: {
    position: 'absolute',
    right: 16,
    bottom: 16,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 64,
  },
  emptyText: {
    marginTop: 16,
    marginBottom: 24,
    color: 'gray',
  },
  emptyButton: {
    marginTop: 8,
  },
});

